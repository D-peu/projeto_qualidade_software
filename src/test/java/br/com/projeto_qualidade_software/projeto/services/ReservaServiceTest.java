package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.jakarta.persistence.validation.mode=none",
        "spring.flyway.enabled=false"
})
@Import(ReservaService.class)
class ReservaServiceTest {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void blocksApprovalWhenAnotherConfirmedReservationOverlaps() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        Reserva confirmada = persistReserva(
                sala,
                StatusReserva.APROVADA,
                LocalDateTime.of(2026, 11, 12, 9, 0),
                LocalDateTime.of(2026, 11, 12, 10, 0),
                responsavel
        );
        Reserva solicitada = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                LocalDateTime.of(2026, 11, 12, 9, 30),
                LocalDateTime.of(2026, 11, 12, 10, 30),
                null
        );
        entityManager.flush();

        assertThrows(
                ReservaService.ReservaIndisponivelException.class,
                () -> reservaService.aprovar(solicitada.getId(), responsavel)
        );

        entityManager.flush();
        assertEquals(
                StatusReserva.SOLICITADA,
                reservaRepository.findById(solicitada.getId()).orElseThrow().getStatus()
        );
        assertEquals(StatusReserva.APROVADA, confirmada.getStatus());
    }

    @Test
    void allowsApprovalWhenOnlyOverlappingReservationsAreStillPending() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        Reserva outraSolicitada = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                LocalDateTime.of(2026, 11, 12, 9, 0),
                LocalDateTime.of(2026, 11, 12, 10, 0),
                null
        );
        Reserva solicitada = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                LocalDateTime.of(2026, 11, 12, 9, 30),
                LocalDateTime.of(2026, 11, 12, 10, 30),
                null
        );
        entityManager.flush();

        Reserva aprovada = reservaService.aprovar(solicitada.getId(), responsavel);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
        assertEquals(StatusReserva.SOLICITADA, outraSolicitada.getStatus());
    }

    @Test
    void blocksApprovalWhenRoomIsInMaintenance() {
        Sala sala = persistSala(StatusSala.EM_MANUTENCAO);
        Responsavel responsavel = persistResponsavel();
        Reserva solicitada = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                LocalDateTime.of(2026, 11, 12, 9, 0),
                LocalDateTime.of(2026, 11, 12, 10, 0),
                null
        );
        entityManager.flush();

        assertThrows(
                ReservaService.ReservaIndisponivelException.class,
                () -> reservaService.aprovar(solicitada.getId(), responsavel)
        );

        assertEquals(StatusReserva.SOLICITADA, solicitada.getStatus());
    }

    private Sala persistSala(StatusSala status) {
        Sala sala = new Sala(null, 30, status);
        entityManager.persist(sala);
        return sala;
    }

    private Responsavel persistResponsavel() {
        Responsavel responsavel = new Responsavel(
                null,
                "Responsável",
                "responsavel@example.test",
                "hash",
                LocalDate.now(),
                LocalDate.now()
        );
        entityManager.persist(responsavel);
        return responsavel;
    }

    private Reserva persistReserva(
            Sala sala,
            StatusReserva status,
            LocalDateTime inicio,
            LocalDateTime fim,
            Responsavel responsavel
    ) {
        Solicitante solicitante = new Solicitante(
                null,
                "Solicitante",
                "solicitante" + System.nanoTime() + "@example.test",
                "hash",
                TipoSolicitante.PROFESSOR,
                LocalDate.now(),
                LocalDate.now()
        );
        entityManager.persist(solicitante);

        Reserva reserva = new Reserva(
                null,
                sala,
                solicitante,
                responsavel,
                inicio,
                fim,
                status,
                LocalDateTime.now(),
                status == StatusReserva.APROVADA ? LocalDateTime.now() : null
        );
        entityManager.persist(reserva);
        return reserva;
    }
}
