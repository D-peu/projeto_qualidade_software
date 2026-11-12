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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.jakarta.persistence.validation.mode=none",
        "spring.flyway.enabled=false"
})
@Import(ReservaServiceTest.FixedTimeConfiguration.class)
class ReservaServiceTest {

    private static final ZoneId TIME_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime REFERENCE_TIME =
            LocalDateTime.of(2026, 11, 12, 8, 0);

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void expiresSolicitationsBeforeAtAndAfterStartWhenListing() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Reserva anterior = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME.minusSeconds(1),
                REFERENCE_TIME.plusHours(1),
                null
        );
        Reserva noInicio = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME,
                REFERENCE_TIME.plusHours(1),
                null
        );
        Reserva futura = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME.plusSeconds(1),
                REFERENCE_TIME.plusHours(1),
                null
        );
        entityManager.flush();

        List<Reserva> reservas = reservaService.listarReservas();

        assertEquals(StatusReserva.EXPIRADA, statusIn(reservas, anterior.getId()));
        assertEquals(StatusReserva.EXPIRADA, statusIn(reservas, noInicio.getId()));
        assertEquals(StatusReserva.SOLICITADA, statusIn(reservas, futura.getId()));

        entityManager.clear();
        assertEquals(StatusReserva.EXPIRADA, reservaRepository.findById(anterior.getId())
                .orElseThrow().getStatus());
        assertEquals(StatusReserva.EXPIRADA, reservaRepository.findById(noInicio.getId())
                .orElseThrow().getStatus());
        assertEquals(StatusReserva.SOLICITADA, reservaRepository.findById(futura.getId())
                .orElseThrow().getStatus());
    }

    @Test
    void expiresSolicitationsWhenFindingAReservation() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Reserva vencida = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME.minusMinutes(1),
                REFERENCE_TIME.plusHours(1),
                null
        );
        entityManager.flush();

        Reserva encontrada = reservaService.buscarReserva(vencida.getId()).orElseThrow();

        assertEquals(StatusReserva.EXPIRADA, encontrada.getStatus());
        entityManager.clear();
        assertEquals(StatusReserva.EXPIRADA, reservaRepository.findById(vencida.getId())
                .orElseThrow().getStatus());
    }

    @Test
    void doesNotChangeReservationsOutsideRequestedState() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        EnumSet<StatusReserva> estadosNaoSolicitados =
                EnumSet.complementOf(EnumSet.of(StatusReserva.SOLICITADA));
        Map<Long, StatusReserva> estadosOriginais = new HashMap<>();
        for (StatusReserva status : estadosNaoSolicitados) {
            Reserva reserva = persistReserva(
                    sala,
                    status,
                    REFERENCE_TIME.minusMinutes(1),
                    REFERENCE_TIME.plusHours(1),
                    null
            );
            estadosOriginais.put(reserva.getId(), status);
        }
        entityManager.flush();

        reservaService.listarReservas();
        entityManager.clear();

        for (Map.Entry<Long, StatusReserva> entry : estadosOriginais.entrySet()) {
            assertEquals(
                    entry.getValue(),
                    reservaRepository.findById(entry.getKey()).orElseThrow().getStatus()
            );
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void persistsExpirationWhenLateApprovalIsRejected() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Long> reservaIds = transaction.execute(status -> {
            Sala sala = persistSala(StatusSala.DISPONIVEL);
            Reserva noInicio = persistReserva(
                    sala,
                    StatusReserva.SOLICITADA,
                    REFERENCE_TIME,
                    REFERENCE_TIME.plusHours(1),
                    null
            );
            Reserva aposInicio = persistReserva(
                    sala,
                    StatusReserva.SOLICITADA,
                    REFERENCE_TIME.minusSeconds(1),
                    REFERENCE_TIME.plusHours(1),
                    null
            );
            entityManager.flush();
            return List.of(noInicio.getId(), aposInicio.getId());
        });

        Responsavel responsavel = transaction.execute(status -> persistResponsavel());
        for (Long reservaId : reservaIds) {
            assertThrows(
                    ReservaService.ReservaExpiradaException.class,
                    () -> reservaService.aprovar(reservaId, responsavel)
            );

            StatusReserva statusPersistido = transaction.execute(status ->
                    reservaRepository.findById(reservaId).orElseThrow().getStatus()
            );
            assertEquals(StatusReserva.EXPIRADA, statusPersistido);
        }
    }

    @Test
    void expiredReservationDoesNotBlockApproval() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        persistReserva(
                sala,
                StatusReserva.EXPIRADA,
                REFERENCE_TIME.minusHours(1),
                REFERENCE_TIME.plusMinutes(30),
                null
        );
        Reserva solicitada = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME.plusMinutes(15),
                REFERENCE_TIME.plusHours(1),
                null
        );
        entityManager.flush();

        Reserva aprovada = reservaService.aprovar(solicitada.getId(), responsavel);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
    }

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

    private StatusReserva statusIn(List<Reserva> reservas, Long reservaId) {
        return reservas.stream()
                .filter(reserva -> reserva.getId().equals(reservaId))
                .findFirst()
                .orElseThrow()
                .getStatus();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {

        @Bean
        ReservaService reservaService(
                ReservaRepository reservaRepository,
                EntityManager entityManager
        ) {
            Clock clock = Clock.fixed(REFERENCE_TIME.atZone(TIME_ZONE).toInstant(), TIME_ZONE);
            return new ReservaService(reservaRepository, entityManager, clock);
        }
    }
}
