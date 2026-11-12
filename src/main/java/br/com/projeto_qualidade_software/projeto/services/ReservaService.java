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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ReservaService {

    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_HORARIO =
            EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO);

    private final ReservaRepository reservaRepository;
    private final EntityManager entityManager;

    public ReservaService(ReservaRepository reservaRepository, EntityManager entityManager) {
        this.reservaRepository = reservaRepository;
        this.entityManager = entityManager;
    }

    @Transactional
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

        if (sala.getStatusSala() == StatusSala.EM_MANUTENCAO
                || existeConflito(reserva, reservasDaSala)) {
            throw new ReservaIndisponivelException(
                    "O recurso ou horário não está mais disponível para esta reserva."
            );
        }

        reserva.aprovar(responsavel);
        return reserva;
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
}
