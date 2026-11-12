package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public ReservaService(ReservaRepository reservaRepository, EntityManager entityManager) {
        this(
                reservaRepository,
                entityManager,
                Clock.system(ZoneId.of("America/Sao_Paulo"))
        );
    }

    ReservaService(
            ReservaRepository reservaRepository,
            EntityManager entityManager,
            Clock clock
    ) {
        this.reservaRepository = reservaRepository;
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
        return reservasDaSala.stream().anyMatch(outra ->
                !Objects.equals(outra.getId(), reserva.getId())
                        && STATUS_QUE_OCUPAM_HORARIO.contains(outra.getStatus())
                        && reserva.getInicio().isBefore(outra.getFim())
                        && reserva.getFim().isAfter(outra.getInicio())
        );
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
