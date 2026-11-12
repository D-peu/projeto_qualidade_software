package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.AlteracaoHorarioAplicada;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.repositories.AlteracaoHorarioAplicadaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservaService {

    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_HORARIO =
            EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO);

    private final ReservaRepository reservaRepository;
    private final AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public ReservaService(
            ReservaRepository reservaRepository,
            AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository,
            EntityManager entityManager
    ) {
        this(
                reservaRepository,
                alteracaoHorarioAplicadaRepository,
                entityManager,
                Clock.system(ZoneId.of("America/Sao_Paulo"))
        );
    }

    ReservaService(
            ReservaRepository reservaRepository,
            AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository,
            EntityManager entityManager,
            Clock clock
    ) {
        this.reservaRepository = reservaRepository;
        this.alteracaoHorarioAplicadaRepository = alteracaoHorarioAplicadaRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public List<Reserva> listarReservas() {
        expirarSolicitacoesVencidas();
        return reservaRepository.findAll();
    }

    @Transactional
    public Optional<Reserva> buscarReserva(Long reservaId) {
        expirarSolicitacoesVencidas();
        return reservaRepository.findById(reservaId);
    }

    @Transactional
    public Reserva aplicarAlteracaoAprovada(
            Long reservaId,
            LocalDateTime novoInicio,
            LocalDateTime novoFim,
            Responsavel responsavel
    ) {
        Long salaId = reservaRepository.findSalaIdByReservaId(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        Sala sala = entityManager.find(Sala.class, salaId, LockModeType.PESSIMISTIC_WRITE);
        if (sala == null) {
            throw new EntityNotFoundException("Sala da reserva não encontrada.");
        }

        List<Reserva> reservasDaSala = reservaRepository.findAllBySalaIdForUpdate(salaId);
        Reserva reserva = reservasDaSala.stream()
                .filter(item -> Objects.equals(item.getId(), reservaId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        if (reserva.getStatus() != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas aprovadas podem ter o horário alterado."
            );
        }

        if (Objects.equals(reserva.getInicio(), novoInicio)
                && Objects.equals(reserva.getFim(), novoFim)) {
            return reserva;
        }

        validarNovoIntervalo(novoInicio, novoFim);

        if (sala.getStatusSala() == StatusSala.EM_MANUTENCAO
                || existeConflito(reserva, novoInicio, novoFim, reservasDaSala)) {
            throw new ReservaIndisponivelException(
                    "O recurso ou horário não está mais disponível para esta reserva."
            );
        }

        if (responsavel == null || responsavel.getId() == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o responsável pela alteração."
            );
        }
        Responsavel responsavelPersistido = entityManager.find(
                Responsavel.class,
                responsavel.getId()
        );
        if (responsavelPersistido == null) {
            throw new EntityNotFoundException("Responsável pela alteração não encontrado.");
        }

        LocalDateTime inicioAnterior = reserva.getInicio();
        LocalDateTime fimAnterior = reserva.getFim();
        Instant aplicadoEm = clock.instant();
        AlteracaoHorarioAplicada alteracao = new AlteracaoHorarioAplicada(
                reserva,
                responsavelPersistido,
                inicioAnterior,
                fimAnterior,
                novoInicio,
                novoFim,
                aplicadoEm
        );

        int reservasAtualizadas = reservaRepository.atualizarHorarioAprovado(
                reservaId,
                StatusReserva.APROVADA,
                novoInicio,
                novoFim
        );
        if (reservasAtualizadas != 1) {
            throw new ReservaIndisponivelException(
                    "A reserva deixou de estar disponível para alteração."
            );
        }

        entityManager.refresh(reserva);
        alteracaoHorarioAplicadaRepository.save(alteracao);
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<AlteracaoHorarioAplicada> consultarHistorico(Long reservaId) {
        if (!reservaRepository.existsById(reservaId)) {
            throw new EntityNotFoundException("Reserva não encontrada.");
        }
        return alteracaoHorarioAplicadaRepository
                .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reservaId);
    }

    @Transactional(noRollbackFor = ReservaExpiradaException.class)
    public Reserva aprovar(Long reservaId, Responsavel responsavel) {
        Long salaId = reservaRepository.findSalaIdByReservaId(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        Sala sala = entityManager.find(Sala.class, salaId, LockModeType.PESSIMISTIC_WRITE);
        if (sala == null) {
            throw new EntityNotFoundException("Sala da reserva não encontrada.");
        }

        List<Reserva> reservasDaSala = reservaRepository.findAllBySalaIdForUpdate(salaId);
        Reserva reserva = reservasDaSala.stream()
                .filter(item -> Objects.equals(item.getId(), reservaId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        if (reserva.getStatus() == StatusReserva.SOLICITADA
                && !reserva.getInicio().isAfter(LocalDateTime.now(clock))) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            entityManager.flush();
            throw new ReservaExpiradaException(
                    "A solicitação expirou antes de ser aprovada."
            );
        }

        if (sala.getStatusSala() == StatusSala.EM_MANUTENCAO
                || existeConflito(reserva, reservasDaSala)) {
            throw new ReservaIndisponivelException(
                    "O recurso ou horário não está mais disponível para esta reserva."
            );
        }

        reserva.aprovar(responsavel);
        return reserva;
    }

    private void expirarSolicitacoesVencidas() {
        reservaRepository.expirarSolicitacoesAte(
                LocalDateTime.now(clock),
                StatusReserva.SOLICITADA,
                StatusReserva.EXPIRADA
        );
    }

    private boolean existeConflito(Reserva reserva, List<Reserva> reservasDaSala) {
        return existeConflito(
                reserva,
                reserva.getInicio(),
                reserva.getFim(),
                reservasDaSala
        );
    }

    private boolean existeConflito(
            Reserva reserva,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<Reserva> reservasDaSala
    ) {
        return reservasDaSala.stream().anyMatch(outra ->
                !Objects.equals(outra.getId(), reserva.getId())
                        && STATUS_QUE_OCUPAM_HORARIO.contains(outra.getStatus())
                        && inicio.isBefore(outra.getFim())
                        && fim.isAfter(outra.getInicio())
        );
    }

    private void validarNovoIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new ReservaHorarioInvalidoException(
                    "O término deve ser posterior ao início."
            );
        }
        if (!inicio.isAfter(LocalDateTime.now(clock))) {
            throw new ReservaHorarioInvalidoException(
                    "O novo horário deve iniciar no futuro."
            );
        }
        if (inicio.getMinute() % 30 != 0
                || fim.getMinute() % 30 != 0
                || inicio.getSecond() != 0
                || fim.getSecond() != 0
                || inicio.getNano() != 0
                || fim.getNano() != 0) {
            throw new ReservaHorarioInvalidoException(
                    "Os horários devem respeitar incrementos de 30 minutos."
            );
        }
    }

    public static class ReservaHorarioInvalidoException extends RuntimeException {

        public ReservaHorarioInvalidoException(String message) {
            super(message);
        }
    }

    public static class ReservaIndisponivelException extends RuntimeException {

        public ReservaIndisponivelException(String message) {
            super(message);
        }
    }

    public static class ReservaExpiradaException extends RuntimeException {

        public ReservaExpiradaException(String message) {
            super(message);
        }
    }
}
