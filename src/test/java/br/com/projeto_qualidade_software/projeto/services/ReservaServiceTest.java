package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.AlteracaoHorarioAplicada;
import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.AlteracaoHorarioAplicadaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.AuditoriaReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @MockitoSpyBean
    private ReservaRepository reservaRepository;

    @MockitoSpyBean
    private SalaRepository salaRepository;

    @MockitoSpyBean
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository;

    @MockitoSpyBean
    private AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaSpy;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long salaId;
    private Long solicitanteId;

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

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-101–113 / RF07 / RN01 - rejeita intervalos inválidos na criação")
    @MethodSource("intervalosInvalidosRF07")
    void solicitarReservaRejeitaIntervalosInvalidosRF07(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim,
            String mensagem
    ) {
        prepararSolicitacaoSemConflito(inicio, fim);

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        salaId, solicitanteId, TipoSolicitante.PROFESSOR, inicio, fim
                )
        );

        assertTrue(erro.getMessage().contains(mensagem), caso);
        verificarNenhumRepositorioConsultado();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-114–115 / RF07 / RN01 - aceita intervalos futuros válidos na criação")
    @MethodSource("intervalosValidosRF07")
    void solicitarReservaAceitaIntervalosFuturosEmMeiaHora(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        prepararSolicitacaoSemConflito(inicio, fim);

        Reserva resultado = reservaService.solicitarReserva(
                salaId, solicitanteId, TipoSolicitante.PROFESSOR, inicio, fim
        );

        assertEquals(inicio, resultado.getInicio(), caso);
        assertEquals(fim, resultado.getFim(), caso);
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

    @Test
    void appliesApprovedScheduleChangeAndPersistsAuditHistory() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        LocalDateTime inicioAnterior = REFERENCE_TIME.plusHours(1);
        LocalDateTime fimAnterior = REFERENCE_TIME.plusHours(2);
        Reserva reserva = persistReserva(
                sala,
                StatusReserva.APROVADA,
                inicioAnterior,
                fimAnterior,
                responsavel
        );
        LocalDateTime novoInicio = REFERENCE_TIME.plusHours(3);
        LocalDateTime novoFim = REFERENCE_TIME.plusHours(4);
        entityManager.flush();

        Reserva atualizada = reservaService.aplicarAlteracaoAprovada(
                reserva.getId(),
                novoInicio,
                novoFim,
                responsavel
        );

        assertEquals(novoInicio, atualizada.getInicio());
        assertEquals(novoFim, atualizada.getFim());
        entityManager.clear();

        List<AlteracaoHorarioAplicada> historico =
                reservaService.consultarHistorico(reserva.getId());
        assertEquals(1, historico.size());
        AlteracaoHorarioAplicada alteracao = historico.get(0);
        assertEquals(reserva.getId(), alteracao.getReserva().getId());
        assertEquals(responsavel.getId(), alteracao.getResponsavel().getId());
        assertEquals(inicioAnterior, alteracao.getOldInicio());
        assertEquals(fimAnterior, alteracao.getOldFim());
        assertEquals(novoInicio, alteracao.getNewInicio());
        assertEquals(novoFim, alteracao.getNewFim());
        assertEquals(REFERENCE_TIME.atZone(TIME_ZONE).toInstant(), alteracao.getAppliedAt());
    }

    @Test
    void doesNotCreateAuditEntryWhenScheduleIsUnchanged() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        LocalDateTime inicio = REFERENCE_TIME.plusHours(1);
        LocalDateTime fim = REFERENCE_TIME.plusHours(2);
        Reserva reserva = persistReserva(
                sala,
                StatusReserva.APROVADA,
                inicio,
                fim,
                responsavel
        );
        entityManager.flush();

        Reserva unchanged = reservaService.aplicarAlteracaoAprovada(
                reserva.getId(),
                inicio,
                fim,
                responsavel
        );

        assertEquals(inicio, unchanged.getInicio());
        assertEquals(fim, unchanged.getFim());
        assertEquals(
                0,
                alteracaoHorarioAplicadaRepository
                        .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reserva.getId())
                        .size()
        );
    }

    @Test
    void rejectsScheduleChangeForReservationThatIsNotApproved() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        Reserva reserva = persistReserva(
                sala,
                StatusReserva.SOLICITADA,
                REFERENCE_TIME.plusHours(1),
                REFERENCE_TIME.plusHours(2),
                null
        );
        entityManager.flush();

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.aplicarAlteracaoAprovada(
                        reserva.getId(),
                        REFERENCE_TIME.plusHours(3),
                        REFERENCE_TIME.plusHours(4),
                        responsavel
                )
        );
        assertEquals(
                0,
                alteracaoHorarioAplicadaRepository
                        .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reserva.getId())
                        .size()
        );
    }

    @Test
    void rejectsInvalidScheduleChangeWithoutPersistingIt() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        LocalDateTime inicioAnterior = REFERENCE_TIME.plusHours(1);
        LocalDateTime fimAnterior = REFERENCE_TIME.plusHours(2);
        Reserva reserva = persistReserva(
                sala,
                StatusReserva.APROVADA,
                inicioAnterior,
                fimAnterior,
                responsavel
        );
        entityManager.flush();

        assertThrows(
                ReservaService.ReservaHorarioInvalidoException.class,
                () -> reservaService.aplicarAlteracaoAprovada(
                        reserva.getId(),
                        REFERENCE_TIME.plusHours(3).plusMinutes(15),
                        REFERENCE_TIME.plusHours(4),
                        responsavel
                )
        );
        assertEquals(inicioAnterior, reservaRepository.findById(reserva.getId())
                .orElseThrow().getInicio());
        assertEquals(
                0,
                alteracaoHorarioAplicadaRepository
                        .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reserva.getId())
                        .size()
        );
    }

    @Test
    void rejectsScheduleChangeThatConflictsWithAnotherApprovedReservation() {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Responsavel responsavel = persistResponsavel();
        Reserva reserva = persistReserva(
                sala,
                StatusReserva.APROVADA,
                REFERENCE_TIME.plusHours(1),
                REFERENCE_TIME.plusHours(2),
                responsavel
        );
        persistReserva(
                sala,
                StatusReserva.APROVADA,
                REFERENCE_TIME.plusHours(3),
                REFERENCE_TIME.plusHours(4),
                responsavel
        );
        entityManager.flush();

        assertThrows(
                ReservaService.ReservaIndisponivelException.class,
                () -> reservaService.aplicarAlteracaoAprovada(
                        reserva.getId(),
                        REFERENCE_TIME.plusHours(3).plusMinutes(30),
                        REFERENCE_TIME.plusHours(4).plusMinutes(30),
                        responsavel
                )
        );
        assertEquals(
                0,
                alteracaoHorarioAplicadaRepository
                        .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reserva.getId())
                        .size()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackScheduleUpdateWhenAuditPersistenceFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Long> ids = transaction.execute(status -> {
            Sala sala = persistSala(StatusSala.DISPONIVEL);
            Responsavel responsavel = persistResponsavel();
            Reserva reserva = persistReserva(
                    sala,
                    StatusReserva.APROVADA,
                    REFERENCE_TIME.plusHours(1),
                    REFERENCE_TIME.plusHours(2),
                    responsavel
            );
            entityManager.flush();
            return List.of(reserva.getId(), responsavel.getId());
        });
        Long reservaId = ids.get(0);
        Long responsavelId = ids.get(1);
        LocalDateTime inicioOriginal = REFERENCE_TIME.plusHours(1);
        LocalDateTime fimOriginal = REFERENCE_TIME.plusHours(2);
        LocalDateTime novoInicio = REFERENCE_TIME.plusHours(3);
        LocalDateTime novoFim = REFERENCE_TIME.plusHours(4);
        doThrow(new IllegalStateException("simulated audit storage failure"))
                .when(alteracaoHorarioAplicadaSpy)
                .save(any(AlteracaoHorarioAplicada.class));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.aplicarAlteracaoAprovada(
                        reservaId,
                        novoInicio,
                        novoFim,
                        new Responsavel(
                                responsavelId,
                                "Responsável",
                                "responsavel@example.test",
                                "hash",
                                LocalDate.now(),
                                LocalDate.now()
                        )
                )
        );
        reset(alteracaoHorarioAplicadaSpy);

        transaction.execute(status -> {
            Reserva persistida = reservaRepository.findById(reservaId).orElseThrow();
            assertEquals(inicioOriginal, persistida.getInicio());
            assertEquals(fimOriginal, persistida.getFim());
            assertTrue(alteracaoHorarioAplicadaRepository
                    .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reservaId)
                    .isEmpty());
            return null;
        });
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
                "responsavel" + System.nanoTime() + "@example.test",
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

    private void prepararSolicitacaoSemConflito(
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        Sala sala = persistSala(StatusSala.DISPONIVEL);
        Solicitante solicitante = new Solicitante(
                null,
                "Solicitante RF07",
                "rf07-" + System.nanoTime() + "@example.test",
                "hash",
                TipoSolicitante.PROFESSOR,
                LocalDate.now(),
                LocalDate.now()
        );
        entityManager.persist(solicitante);
        entityManager.flush();
        salaId = sala.getId();
        solicitanteId = solicitante.getId();
    }

    private void verificarNenhumRepositorioConsultado() {
        verifyNoInteractions(reservaRepository, salaRepository, solicitanteRepository);
    }

    private static Stream<Arguments> intervalosInvalidosRF07() {
        LocalDateTime futuro = REFERENCE_TIME.plusDays(1);
        return Stream.of(
                Arguments.of(
                        "início passado",
                        REFERENCE_TIME.minusHours(1),
                        REFERENCE_TIME.plusHours(1),
                        "futuro"
                ),
                Arguments.of(
                        "início exatamente agora",
                        REFERENCE_TIME,
                        REFERENCE_TIME.plusHours(1),
                        "futuro"
                ),
                Arguments.of(
                        "início em minuto diferente de 00 ou 30",
                        REFERENCE_TIME.plusHours(1).plusMinutes(15),
                        REFERENCE_TIME.plusHours(2).plusMinutes(15),
                        "30 minutos"
                ),
                Arguments.of(
                        "fim em minuto diferente de 00 ou 30",
                        REFERENCE_TIME.plusHours(1),
                        REFERENCE_TIME.plusHours(2).plusMinutes(15),
                        "30 minutos"
                ),
                Arguments.of("fim igual ao início", futuro, futuro, "posterior"),
                Arguments.of(
                        "fim anterior ao início",
                        futuro.plusHours(1),
                        futuro,
                        "posterior"
                )
        );
    }

    private static Stream<Arguments> intervalosValidosRF07() {
        return Stream.of(
                Arguments.of(
                        "horário futuro no mesmo dia",
                        REFERENCE_TIME.plusHours(1),
                        REFERENCE_TIME.plusHours(2)
                ),
                Arguments.of(
                        "horário futuro em outro dia",
                        REFERENCE_TIME.plusDays(1).withHour(9).withMinute(30),
                        REFERENCE_TIME.plusDays(1).withHour(10).withMinute(30)
                )
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {

        @Bean
        ReservaService reservaService(
                ReservaRepository reservaRepository,
                AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository,
                AuditoriaReservaRepository auditoriaReservaRepository,
                SalaRepository salaRepository,
                SolicitanteRepository solicitanteRepository,
                ResponsavelRepository responsavelRepository,
                AdministradorRepository administradorRepository,
                MaterialRepository materialRepository,
                EntityManager entityManager
        ) {
            Clock clock = Clock.fixed(REFERENCE_TIME.atZone(TIME_ZONE).toInstant(), TIME_ZONE);
            return new ReservaService(
                    reservaRepository,
                    alteracaoHorarioAplicadaRepository,
                    auditoriaReservaRepository,
                    salaRepository,
                    solicitanteRepository,
                    responsavelRepository,
                    administradorRepository,
                    materialRepository,
                    entityManager,
                    clock
            );
        }
    }

    private enum TipoConflitoAprovacao {
        SALA("sala"),
        PROFESSOR("professor"),
        MATERIAL("material");

        private final String mensagem;

        TipoConflitoAprovacao(String mensagem) {
            this.mensagem = mensagem;
        }
    }
}
