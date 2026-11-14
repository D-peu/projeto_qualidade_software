package br.com.projeto_qualidade_software.projeto.integration;

import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AuditoriaReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.ContextoAutenticacao;
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;
import br.com.projeto_qualidade_software.projeto.security.UsuarioAutenticado;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;
import br.com.projeto_qualidade_software.projeto.services.ReservaService;
import br.com.projeto_qualidade_software.projeto.services.NotificacaoClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.auth.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Testcontainers
@Transactional
class ReservaPostgresIntegrationTest {

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2040, 1, 2, 10, 0);
    private static final LocalDateTime FIM =
            LocalDateTime.of(2040, 1, 2, 11, 0);
    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_SALA = Set.of(
            StatusReserva.SOLICITADA,
            StatusReserva.APROVADA,
            StatusReserva.EM_USO,
            StatusReserva.CONCLUIDA,
            StatusReserva.NAO_COMPARECEU
    );

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("reservas_test")
                    .withUsername("test")
                    .withPassword("test")
                    .waitingFor(Wait.forSuccessfulCommand(
                            "pg_isready -h 127.0.0.1 -U test -d reservas_test"
                    ).withStartupTimeout(Duration.ofSeconds(60)));

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AuditoriaReservaRepository auditoriaReservaRepository;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NotificacaoClient notificacaoClient;

    @AfterEach
    void limparContextoAutenticacao() {
        ContextoAutenticacao.limpar();
    }

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveImpedirSolicitacoesConcorrentesParaMesmoProfessorEmSalasDiferentes()
            throws Exception {
        FixtureConcorrenciaRecursos fixture = criarFixtureConcorrenciaRecursos();
        try {
            List<ResultadoConcorrencia> resultados = solicitarRecursosSimultaneamente(
                    fixture,
                    List.of(fixture.professorId()),
                    List.of()
            );

            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.reserva() != null)
                    .count());
            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.conflito() != null)
                    .count());
            assertEquals(
                    1L,
                    reservaRepository.contarConflitosPorProfessor(
                            fixture.professorId(),
                            STATUS_QUE_OCUPAM_SALA,
                            INICIO,
                            FIM,
                            null
                    )
            );
        } finally {
            limparFixtureConcorrenciaRecursos(fixture);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveImpedirSolicitacoesConcorrentesParaMesmoMaterialEmSalasDiferentes()
            throws Exception {
        FixtureConcorrenciaRecursos fixture = criarFixtureConcorrenciaRecursos();
        try {
            List<ResultadoConcorrencia> resultados = solicitarRecursosSimultaneamente(
                    fixture,
                    List.of(),
                    List.of(fixture.materialId())
            );

            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.reserva() != null)
                    .count());
            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.conflito() != null)
                    .count());
            assertEquals(
                    1L,
                    reservaRepository.contarConflitosPorMaterial(
                            fixture.materialId(),
                            STATUS_QUE_OCUPAM_SALA,
                            INICIO,
                            FIM,
                            null
                    )
            );
        } finally {
            limparFixtureConcorrenciaRecursos(fixture);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("CP-128 / RF09 / RN03 / INV-03 - Apenas uma aprovação concorrente confirma conflito")
    void deveAprovarNoMaximoUmaReservaConflitanteEmOperacoesConcorrentes()
            throws Exception {
        FixtureAprovacaoConcorrente fixture = criarFixtureAprovacaoConcorrente();
        try {
            List<ResultadoAprovacaoConcorrente> resultados =
                    aprovarReservasSimultaneamente(fixture);

            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.reserva() != null)
                    .count());
            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.conflito() != null)
                    .count());
            assertEquals(
                    1L,
                    reservaRepository.contarConflitos(
                            fixture.salaId(),
                            Set.of(
                                    StatusReserva.APROVADA,
                                    StatusReserva.EM_USO,
                                    StatusReserva.CONCLUIDA,
                                    StatusReserva.NAO_COMPARECEU
                            ),
                            INICIO,
                            FIM,
                            null
                    )
            );
        } finally {
            limparFixtureAprovacaoConcorrente(fixture);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("CP-162 / RF06 / RF08 / RF09 / RN03 / INV-03 - Apenas uma alteração concorrente aceita o mesmo horário")
    void deveAceitarNoMaximoUmaAlteracaoConcorrenteParaMesmoHorario()
            throws Exception {
        FixtureAlteracaoConcorrente fixture = criarFixtureAlteracaoConcorrente();
        LocalDateTime novoInicio = FIM.plusHours(2);
        LocalDateTime novoFim = novoInicio.plusHours(1);
        try {
            List<ResultadoConcorrencia> resultados = alterarHorariosSimultaneamente(
                    fixture,
                    novoInicio,
                    novoFim
            );

            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.reserva() != null)
                    .count());
            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado.conflito() != null)
                    .count());
            assertEquals(
                    1L,
                    reservaRepository.contarConflitos(
                            fixture.salaId(),
                            STATUS_QUE_OCUPAM_SALA,
                            novoInicio,
                            novoFim,
                            null
                    )
            );
        } finally {
            limparFixtureAlteracaoConcorrente(fixture);
        }
    }

    @Test
    void devePersistirReservaERecuperarSuasAssociacoes() {
        // Rastreabilidade: RF07, INV-01 e CB-47 (reserva válida persistida no PostgreSQL).
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        Reserva reserva = new Reserva(
                null, sala, solicitante, null, INICIO, FIM, null, null, null
        );

        Reserva salva = reservaRepository.saveAndFlush(reserva);
        entityManager.clear();

        Reserva recuperada = reservaRepository.findById(salva.getId()).orElseThrow();

        assertEquals(salva.getId(), recuperada.getId());
        assertEquals(StatusReserva.SOLICITADA, recuperada.getStatus());
        assertEquals(INICIO, recuperada.getInicio());
        assertEquals(FIM, recuperada.getFim());
        assertEquals(sala.getId(), recuperada.getSala().getId());
        assertEquals(solicitante.getId(), recuperada.getSolicitante().getId());
        assertNotNull(recuperada.getSolicitadoEm());
    }

    @Test
    @DisplayName("CP-55 / RF10 / INV-04 - Administrador não aprova material restrito")
    void deveRecusarAprovacaoDeMaterialRestritoPorAdministrador() {
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Material material = new Material(
                null,
                "Projetor " + UUID.randomUUID()
        );
        material.setRestrito(true);
        materialRepository.saveAndFlush(material);

        Administrador administrador = new Administrador(
                null,
                "Administrador de integração",
                UUID.randomUUID() + "@teste.local",
                "senha-de-teste",
                null,
                null
        );
        entityManager.persist(administrador);

        Reserva reserva = salvarReserva(
                sala,
                solicitante,
                INICIO,
                FIM,
                StatusReserva.SOLICITADA
        );
        reserva.getMateriais().add(material);
        reservaRepository.saveAndFlush(reserva);

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.aprovarReserva(
                        reserva.getId(),
                        administrador.getId(),
                        TipoAprovadorReserva.ADMINISTRADOR
                )
        );
        assertEquals(
                StatusReserva.SOLICITADA,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    @DisplayName("CP-124 / RF11 / RN04 / INV-05 - Manutenção impede aprovação e preserva solicitação")
    void deveRecusarAprovacaoQuandoSalaEntraEmManutencao() {
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Responsavel responsavel = responsavelRepository.saveAndFlush(new Responsavel(
                null,
                "Responsável de aprovação",
                UUID.randomUUID() + "@teste.local",
                "senha-de-teste",
                null,
                null
        ));
        Reserva reserva = salvarReserva(
                sala,
                solicitante,
                INICIO,
                FIM,
                StatusReserva.SOLICITADA
        );
        sala.setStatusSala(StatusSala.EM_MANUTENCAO);
        salaRepository.saveAndFlush(sala);

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.aprovarReserva(
                        reserva.getId(),
                        responsavel.getId(),
                        TipoAprovadorReserva.RESPONSAVEL
                )
        );

        assertEquals(
                StatusReserva.SOLICITADA,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus()
        );
        assertTrue(
                auditoriaReservaRepository
                        .findByReservaIdOrderByDataHoraAsc(reserva.getId())
                        .isEmpty()
        );
    }

    @Test
    @DisplayName("CP-138 / CB-71 / RF14-RF15 / RN07 - Criação persiste auditoria inicial")
    void devePersistirAuditoriaDaCriacaoNaMesmaTransacao() {
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();

        Reserva criada = reservaService.solicitarReserva(
                sala.getId(),
                solicitante.getId(),
                solicitante.getTipoSolicitante(),
                INICIO,
                FIM
        );
        entityManager.flush();
        entityManager.clear();

        List<AuditoriaReserva> eventos =
                auditoriaReservaRepository.findByReservaIdOrderByDataHoraAsc(criada.getId());

        assertEquals(1, eventos.size());
        assertNull(eventos.getFirst().getEstadoAnterior());
        assertEquals(StatusReserva.SOLICITADA, eventos.getFirst().getNovoEstado());
        assertEquals(solicitante.getId(), eventos.getFirst().getUsuarioId());
        assertEquals(PerfilUsuario.SOLICITANTE, eventos.getFirst().getPerfilUsuario());
        assertNotNull(eventos.getFirst().getDataHora());
    }

    @Test
    @DisplayName("CP-139 / CB-72 / RF02 / RF14-RF15 / RN07 - Conclusão salva estado e auditoria")
    void devePersistirAuditoriaAoConcluirUtilizacao() {
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
        String identificador = UUID.randomUUID().toString();
        Responsavel responsavel = responsavelRepository.saveAndFlush(new Responsavel(
                null,
                "Responsável operacional",
                identificador + "@teste.local",
                "senha-de-teste",
                null,
                null
        ));
        Reserva reserva = salvarReserva(
                sala, solicitante, INICIO, FIM, StatusReserva.EM_USO
        );
        reserva.setResponsavel(responsavel);
        reservaRepository.saveAndFlush(reserva);
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                responsavel.getId(),
                responsavel.getName(),
                responsavel.getEmail(),
                PerfilUsuario.RESPONSAVEL,
                null
        ));

        reservaService.concluirUtilizacao(reserva.getId());
        entityManager.flush();
        entityManager.clear();

        Reserva atualizada = reservaRepository.findById(reserva.getId()).orElseThrow();
        List<AuditoriaReserva> eventos =
                auditoriaReservaRepository.findByReservaIdOrderByDataHoraAsc(reserva.getId());

        assertEquals(StatusReserva.CONCLUIDA, atualizada.getStatus());
        assertEquals(1, eventos.size());
        assertEquals(StatusReserva.EM_USO, eventos.getFirst().getEstadoAnterior());
        assertEquals(StatusReserva.CONCLUIDA, eventos.getFirst().getNovoEstado());
        assertEquals(responsavel.getId(), eventos.getFirst().getUsuarioId());
        assertEquals(PerfilUsuario.RESPONSAVEL, eventos.getFirst().getPerfilUsuario());
        assertNotNull(eventos.getFirst().getDataHora());
    }

    @Test
    @DisplayName("CP-140 / CB-73 / RF08-RF15 / RN02-RN06 - Conclusão não libera intervalo")
    void deveManterConflitoAteOFimOriginalAposConclusaoAntecipada() {
        Sala sala = salvarSala();
        Solicitante solicitanteExistente = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Solicitante novoSolicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
        salvarReserva(
                sala, solicitanteExistente, INICIO, FIM, StatusReserva.CONCLUIDA
        );

        IllegalStateException conflito = assertThrows(
                IllegalStateException.class,
                () -> reservaService.solicitarReserva(
                        sala.getId(),
                        novoSolicitante.getId(),
                        TipoSolicitante.COORDENADOR,
                        INICIO.plusMinutes(30),
                        FIM.plusMinutes(30)
                )
        );

        assertTrue(conflito.getMessage().contains("sala"));
    }

    @Test
    @DisplayName("CP-141 / CB-83 / RF02-RF15 - Solicitante não altera reserva persistida")
    void deveRecusarTransicaoOperacionalDeSolicitanteSemGravarAuditoria() {
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Reserva reserva = salvarReserva(
                sala, solicitante, INICIO, FIM, StatusReserva.EM_USO
        );
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                solicitante.getId(),
                solicitante.getName(),
                solicitante.getEmail(),
                PerfilUsuario.SOLICITANTE,
                solicitante.getTipoSolicitante()
        ));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.concluirUtilizacao(reserva.getId())
        );
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                StatusReserva.EM_USO,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus()
        );
        assertTrue(
                auditoriaReservaRepository
                        .findByReservaIdOrderByDataHoraAsc(reserva.getId())
                        .isEmpty()
        );
    }

    private List<ResultadoConcorrencia> solicitarRecursosSimultaneamente(
            FixtureConcorrenciaRecursos fixture,
            List<Long> professorIds,
            List<Long> materialIds
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        List<Future<ResultadoConcorrencia>> tarefas = new ArrayList<>(2);
        try {
            tarefas.add(executor.submit(() -> solicitarAposBarreira(
                    fixture.primeiraSalaId(),
                    fixture.primeiroSolicitanteId(),
                    TipoSolicitante.COORDENADOR,
                    professorIds,
                    materialIds,
                    prontas,
                    iniciar
            )));
            tarefas.add(executor.submit(() -> solicitarAposBarreira(
                    fixture.segundaSalaId(),
                    fixture.segundoSolicitanteId(),
                    TipoSolicitante.COORDENADOR,
                    professorIds,
                    materialIds,
                    prontas,
                    iniciar
            )));

            assertTrue(prontas.await(10, TimeUnit.SECONDS),
                    "As duas threads devem estar prontas antes da liberação.");
            iniciar.countDown();
            return List.of(
                    obterResultado(tarefas.get(0)),
                    obterResultado(tarefas.get(1))
            );
        } finally {
            iniciar.countDown();
            encerrarExecutor(executor);
        }
    }

    @Test
    void deveListarReservasDoSolicitantePorInicioDecrescente() {
        // Rastreabilidade: RF06, INV-12 e CB-58.
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        Solicitante outroSolicitante = salvarSolicitante();
        Reserva maisAntiga = salvarReserva(
                sala, solicitante, INICIO, FIM, StatusReserva.SOLICITADA
        );
        Reserva intermediaria = salvarReserva(
                sala, solicitante, INICIO.plusDays(1), FIM.plusDays(1),
                StatusReserva.SOLICITADA
        );
        Reserva maisRecente = salvarReserva(
                sala, solicitante, INICIO.plusDays(2), FIM.plusDays(2),
                StatusReserva.SOLICITADA
        );
        salvarReserva(
                sala, outroSolicitante, INICIO.plusDays(3), FIM.plusDays(3),
                StatusReserva.SOLICITADA
        );

        entityManager.clear();
        List<Reserva> reservas =
                reservaRepository.findBySolicitanteIdOrderByInicioDesc(solicitante.getId());

        assertEquals(
                List.of(maisRecente.getId(), intermediaria.getId(), maisAntiga.getId()),
                reservas.stream().map(Reserva::getId).toList()
        );
    }

    @Test
    void deveDetectarConflitoEntreIntervalosSobrepostos() {
        // Rastreabilidade: RF08, RN02, INV-02 e CB-54.
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        salvarReserva(sala, solicitante, INICIO, FIM, StatusReserva.SOLICITADA);

        long conflitos = reservaRepository.contarConflitos(
                sala.getId(),
                STATUS_QUE_OCUPAM_SALA,
                INICIO.plusMinutes(30),
                FIM.plusMinutes(30),
                null
        );

        assertEquals(1L, conflitos);
    }

    @Test
    @DisplayName("CP-97 / RF08 / RN02 - bloqueia sobreposição do mesmo professor")
    void deveImpedirSobreposicaoDoMesmoProfessorEmSalasDiferentes() {
        Sala salaExistente = salvarSala();
        Sala outraSala = salvarSala();
        Solicitante solicitanteExistente = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Solicitante solicitanteNovo = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Solicitante professor = salvarSolicitante(TipoSolicitante.PROFESSOR);
        Reserva existente = salvarReserva(
                salaExistente,
                solicitanteExistente,
                INICIO,
                FIM,
                StatusReserva.SOLICITADA
        );
        existente.setProfessores(Set.of(professor));
        reservaRepository.saveAndFlush(existente);

        IllegalStateException conflito = assertThrows(
                IllegalStateException.class,
                () -> reservaService.solicitarReserva(
                        outraSala.getId(),
                        solicitanteNovo.getId(),
                        TipoSolicitante.COORDENADOR,
                        INICIO.plusMinutes(30),
                        FIM.plusMinutes(30),
                        List.of(professor.getId()),
                        List.of()
                )
        );

        assertTrue(conflito.getMessage().contains("professor"));
    }

    @Test
    @DisplayName("CP-98 / RF08 / RN02 - bloqueia sobreposição do mesmo material")
    void deveImpedirSobreposicaoDoMesmoMaterialEmSalasDiferentes() {
        Sala salaExistente = salvarSala();
        Sala outraSala = salvarSala();
        Solicitante solicitanteExistente = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Solicitante solicitanteNovo = salvarSolicitante(TipoSolicitante.COORDENADOR);
        Material material = materialRepository.saveAndFlush(
                new Material(null, "Material de integração")
        );
        Reserva existente = salvarReserva(
                salaExistente,
                solicitanteExistente,
                INICIO,
                FIM,
                StatusReserva.SOLICITADA
        );
        existente.setMateriais(Set.of(material));
        reservaRepository.saveAndFlush(existente);

        IllegalStateException conflito = assertThrows(
                IllegalStateException.class,
                () -> reservaService.solicitarReserva(
                        outraSala.getId(),
                        solicitanteNovo.getId(),
                        TipoSolicitante.COORDENADOR,
                        INICIO.plusMinutes(30),
                        FIM.plusMinutes(30),
                        List.of(),
                        List.of(material.getId())
                )
        );

        assertTrue(conflito.getMessage().contains("material"));
    }

    @ParameterizedTest(name = "intervalo adjacente {0} não gera conflito")
    @MethodSource("intervalosAdjacentes")
    void naoDeveDetectarConflitoEntreIntervalosAdjacentes(
            LocalDateTime inicioNovo,
            LocalDateTime fimNovo
    ) {
        // Rastreabilidade: RF08, RN02, INV-02, CB-52 e CB-53.
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        salvarReserva(sala, solicitante, INICIO, FIM, StatusReserva.APROVADA);

        long conflitos = reservaRepository.contarConflitos(
                sala.getId(), STATUS_QUE_OCUPAM_SALA, inicioNovo, fimNovo, null
        );

        assertEquals(0L, conflitos);
    }

    private static Stream<Arguments> intervalosAdjacentes() {
        return Stream.of(
                Arguments.of(INICIO.minusHours(1), INICIO),
                Arguments.of(FIM, FIM.plusHours(1))
        );
    }

    @Test
    void deveExcluirAPropriaReservaDaContagemDeConflitos() {
        // Rastreabilidade: RF06, RN02, INV-02 e CB-55.
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        Reserva reserva = salvarReserva(
                sala, solicitante, INICIO, FIM, StatusReserva.SOLICITADA
        );

        long conflitos = reservaRepository.contarConflitos(
                sala.getId(),
                STATUS_QUE_OCUPAM_SALA,
                INICIO,
                FIM,
                reserva.getId()
        );

        assertEquals(0L, conflitos);
    }

    @ParameterizedTest(name = "status existente {0}: bloqueia solicitação = {1}")
    @MethodSource("statusDeOcupacao")
    void deveBloquearSomenteEstadosQueOcupamASala(
            StatusReserva statusExistente,
            boolean deveBloquear
    ) {
        // Rastreabilidade: RF08, RN02, INV-02 e CB-49 a CB-51.
        Sala sala = salvarSala();
        Solicitante solicitante = salvarSolicitante();
        salvarReserva(sala, solicitante, INICIO, FIM, statusExistente);

        if (deveBloquear) {
            assertThrows(
                    IllegalStateException.class,
                    () -> reservaService.solicitarReserva(
                            sala.getId(),
                            solicitante.getId(),
                            TipoSolicitante.PROFESSOR,
                            INICIO,
                            FIM
                    )
            );
            assertEquals(1L, reservaRepository.count());
        } else {
            Reserva criada = assertDoesNotThrow(() -> reservaService.solicitarReserva(
                    sala.getId(),
                    solicitante.getId(),
                    TipoSolicitante.PROFESSOR,
                    INICIO,
                    FIM
            ));
            assertNotNull(criada.getId());
            assertEquals(2L, reservaRepository.count());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveAceitarSomenteUmaReservaEmSolicitacoesConcorrentesComConflito()
            throws Exception {
        // Rastreabilidade: RF09, RN03 e INV-03.
        FixtureConcorrencia fixture = criarFixtureConcorrencia();
        try {
            List<ResultadoConcorrencia> resultados = solicitarSimultaneamente(
                    fixture,
                    new Intervalo(INICIO, FIM),
                    new Intervalo(INICIO, FIM)
            );

            List<ResultadoConcorrencia> aceitas = resultados.stream()
                    .filter(resultado -> resultado.reserva() != null)
                    .toList();
            List<ResultadoConcorrencia> rejeitadas = resultados.stream()
                    .filter(resultado -> resultado.conflito() != null)
                    .toList();

            assertEquals(1, aceitas.size());
            assertEquals(1, rejeitadas.size());
            assertTrue(rejeitadas.getFirst().conflito().getMessage()
                    .contains("Já existe uma reserva para esta sala"));
            assertEquals(
                    1L,
                    contarReservasNoIntervalo(fixture.salaId(), INICIO, FIM)
            );
        } finally {
            limparFixtureConcorrencia(fixture);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveAceitarSolicitacoesConcorrentesParaIntervalosNaoSobrepostos()
            throws Exception {
        // Rastreabilidade: RF09, RN03 e INV-03 (intervalos não conflitantes).
        FixtureConcorrencia fixture = criarFixtureConcorrencia();
        LocalDateTime segundoInicio = FIM.plusHours(1);
        LocalDateTime segundoFim = segundoInicio.plusHours(1);
        try {
            List<ResultadoConcorrencia> resultados = solicitarSimultaneamente(
                    fixture,
                    new Intervalo(INICIO, FIM),
                    new Intervalo(segundoInicio, segundoFim)
            );

            assertEquals(2, resultados.size());
            assertTrue(resultados.stream()
                    .allMatch(resultado -> resultado.reserva() != null
                            && resultado.conflito() == null));
            assertEquals(
                    2L,
                    contarReservasNoIntervalo(fixture.salaId(), INICIO, segundoFim)
            );
        } finally {
            limparFixtureConcorrencia(fixture);
        }
    }

    private FixtureConcorrencia criarFixtureConcorrencia() {
        return novaTransacao().execute(status -> {
            Sala sala = salvarSala();
            Solicitante solicitante = salvarSolicitante();
            return new FixtureConcorrencia(sala.getId(), solicitante.getId());
        });
    }

    private FixtureAlteracaoConcorrente criarFixtureAlteracaoConcorrente() {
        return novaTransacao().execute(status -> {
            Sala sala = salvarSala();
            Solicitante primeiroSolicitante = salvarSolicitante();
            Solicitante segundoSolicitante = salvarSolicitante();
            LocalDateTime segundoInicio = FIM.plusHours(1);
            Reserva primeira = salvarReserva(
                    sala, primeiroSolicitante, INICIO, FIM, StatusReserva.SOLICITADA
            );
            Reserva segunda = salvarReserva(
                    sala,
                    segundoSolicitante,
                    segundoInicio,
                    segundoInicio.plusHours(1),
                    StatusReserva.SOLICITADA
            );
            return new FixtureAlteracaoConcorrente(
                    sala.getId(),
                    primeira.getId(),
                    segunda.getId(),
                    primeiroSolicitante.getId(),
                    segundoSolicitante.getId()
            );
        });
    }

    private void limparFixtureAlteracaoConcorrente(FixtureAlteracaoConcorrente fixture) {
        novaTransacao().executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "delete from auditorias_reserva where reserva_id in (?, ?)",
                    fixture.primeiraReservaId(),
                    fixture.segundaReservaId()
            );
            jdbcTemplate.update(
                    "delete from reservas where id in (?, ?)",
                    fixture.primeiraReservaId(),
                    fixture.segundaReservaId()
            );
            jdbcTemplate.update(
                    "delete from solicitantes where id in (?, ?)",
                    fixture.primeiroSolicitanteId(),
                    fixture.segundoSolicitanteId()
            );
            jdbcTemplate.update("delete from salas where id = ?", fixture.salaId());
        });
    }

    private FixtureConcorrenciaRecursos criarFixtureConcorrenciaRecursos() {
        return novaTransacao().execute(status -> {
            Sala primeiraSala = salvarSala();
            Sala segundaSala = salvarSala();
            Solicitante primeiroSolicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
            Solicitante segundoSolicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
            Solicitante professor = salvarSolicitante(TipoSolicitante.PROFESSOR);
            Material material = materialRepository.saveAndFlush(
                    new Material(null, UUID.randomUUID().toString())
            );
            return new FixtureConcorrenciaRecursos(
                    primeiraSala.getId(),
                    segundaSala.getId(),
                    primeiroSolicitante.getId(),
                    segundoSolicitante.getId(),
                    professor.getId(),
                    material.getId()
            );
        });
    }

    private void limparFixtureConcorrenciaRecursos(FixtureConcorrenciaRecursos fixture) {
        novaTransacao().executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "delete from reservas_professores where reserva_id in "
                            + "(select id from reservas where sala_id in (?, ?))",
                    fixture.primeiraSalaId(),
                    fixture.segundaSalaId()
            );
            jdbcTemplate.update(
                    "delete from reservas_materiais where reserva_id in "
                            + "(select id from reservas where sala_id in (?, ?))",
                    fixture.primeiraSalaId(),
                    fixture.segundaSalaId()
            );
            jdbcTemplate.update(
                    "delete from auditorias_reserva where reserva_id in "
                            + "(select id from reservas where sala_id in (?, ?))",
                    fixture.primeiraSalaId(),
                    fixture.segundaSalaId()
            );
            jdbcTemplate.update(
                    "delete from reservas where sala_id in (?, ?)",
                    fixture.primeiraSalaId(),
                    fixture.segundaSalaId()
            );
            jdbcTemplate.update(
                    "delete from solicitantes where id in (?, ?, ?)",
                    fixture.primeiroSolicitanteId(),
                    fixture.segundoSolicitanteId(),
                    fixture.professorId()
            );
            jdbcTemplate.update("delete from materiais where id = ?", fixture.materialId());
            jdbcTemplate.update(
                    "delete from salas where id in (?, ?)",
                    fixture.primeiraSalaId(),
                    fixture.segundaSalaId()
            );
        });
    }

    private FixtureAprovacaoConcorrente criarFixtureAprovacaoConcorrente() {
        return novaTransacao().execute(status -> {
            Sala sala = salvarSala();
            Solicitante primeiroSolicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
            Solicitante segundoSolicitante = salvarSolicitante(TipoSolicitante.COORDENADOR);
            String identificador = UUID.randomUUID().toString();
            Responsavel responsavel = responsavelRepository.saveAndFlush(new Responsavel(
                    null,
                    "Responsável de integração",
                    identificador + "@teste.local",
                    "senha-de-teste",
                    null,
                    null
            ));
            Reserva primeira = salvarReserva(
                    sala, primeiroSolicitante, INICIO, FIM, StatusReserva.SOLICITADA
            );
            Reserva segunda = salvarReserva(
                    sala, segundoSolicitante, INICIO, FIM, StatusReserva.SOLICITADA
            );
            return new FixtureAprovacaoConcorrente(
                    sala.getId(),
                    primeira.getId(),
                    segunda.getId(),
                    primeiroSolicitante.getId(),
                    segundoSolicitante.getId(),
                    responsavel.getId()
            );
        });
    }

    private void limparFixtureAprovacaoConcorrente(FixtureAprovacaoConcorrente fixture) {
        novaTransacao().executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "delete from auditorias_reserva where reserva_id in (?, ?)",
                    fixture.primeiraReservaId(),
                    fixture.segundaReservaId()
            );
            jdbcTemplate.update(
                    "delete from reservas where id in (?, ?)",
                    fixture.primeiraReservaId(),
                    fixture.segundaReservaId()
            );
            jdbcTemplate.update(
                    "delete from solicitantes where id in (?, ?)",
                    fixture.primeiroSolicitanteId(),
                    fixture.segundoSolicitanteId()
            );
            jdbcTemplate.update("delete from salas where id = ?", fixture.salaId());
            jdbcTemplate.update(
                    "delete from responsaveis where id = ?",
                    fixture.responsavelId()
            );
        });
    }

    private List<ResultadoAprovacaoConcorrente> aprovarReservasSimultaneamente(
            FixtureAprovacaoConcorrente fixture
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        List<Future<ResultadoAprovacaoConcorrente>> tarefas = new ArrayList<>(2);
        try {
            tarefas.add(executor.submit(() -> aprovarAposBarreira(
                    fixture.primeiraReservaId(), fixture.responsavelId(), prontas, iniciar
            )));
            tarefas.add(executor.submit(() -> aprovarAposBarreira(
                    fixture.segundaReservaId(), fixture.responsavelId(), prontas, iniciar
            )));

            assertTrue(prontas.await(10, TimeUnit.SECONDS),
                    "As duas threads devem estar prontas antes da liberação.");
            iniciar.countDown();
            return List.of(
                    tarefas.get(0).get(20, TimeUnit.SECONDS),
                    tarefas.get(1).get(20, TimeUnit.SECONDS)
            );
        } finally {
            iniciar.countDown();
            encerrarExecutor(executor);
        }
    }

    private ResultadoAprovacaoConcorrente aprovarAposBarreira(
            Long reservaId,
            Long responsavelId,
            CountDownLatch prontas,
            CountDownLatch iniciar
    ) throws InterruptedException, TimeoutException {
        prontas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) {
            throw new TimeoutException("A liberação das aprovações concorrentes expirou.");
        }

        try {
            Reserva reserva = reservaService.aprovarReserva(
                    reservaId, responsavelId, TipoAprovadorReserva.RESPONSAVEL
            );
            return new ResultadoAprovacaoConcorrente(reserva, null);
        } catch (IllegalStateException conflito) {
            return new ResultadoAprovacaoConcorrente(null, conflito);
        }
    }

    private void limparFixtureConcorrencia(FixtureConcorrencia fixture) {
        novaTransacao().executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "delete from reservas_professores where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    fixture.salaId()
            );
            jdbcTemplate.update(
                    "delete from reservas_materiais where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    fixture.salaId()
            );
            jdbcTemplate.update(
                    "delete from auditorias_reserva where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    fixture.salaId()
            );
            jdbcTemplate.update("delete from reservas where sala_id = ?", fixture.salaId());
            jdbcTemplate.update(
                    "delete from solicitantes where id = ?",
                    fixture.solicitanteId()
            );
            jdbcTemplate.update("delete from salas where id = ?", fixture.salaId());
        });
    }

    private List<ResultadoConcorrencia> solicitarSimultaneamente(
            FixtureConcorrencia fixture,
            Intervalo primeiro,
            Intervalo segundo
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        List<Future<ResultadoConcorrencia>> tarefas = new ArrayList<>(2);
        try {
            tarefas.add(executor.submit(() ->
                    solicitarAposBarreira(fixture, primeiro, prontas, iniciar)));
            tarefas.add(executor.submit(() ->
                    solicitarAposBarreira(fixture, segundo, prontas, iniciar)));

            assertTrue(prontas.await(10, TimeUnit.SECONDS),
                    "As duas threads devem estar prontas antes da liberação.");
            iniciar.countDown();

            return List.of(
                    obterResultado(tarefas.get(0)),
                    obterResultado(tarefas.get(1))
            );
        } finally {
            iniciar.countDown();
            encerrarExecutor(executor);
        }
    }

    private List<ResultadoConcorrencia> alterarHorariosSimultaneamente(
            FixtureAlteracaoConcorrente fixture,
            LocalDateTime inicio,
            LocalDateTime fim
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        List<Future<ResultadoConcorrencia>> tarefas = new ArrayList<>(2);
        try {
            tarefas.add(executor.submit(() -> alterarAposBarreira(
                    fixture.primeiraReservaId(),
                    fixture.primeiroSolicitanteId(),
                    inicio,
                    fim,
                    prontas,
                    iniciar
            )));
            tarefas.add(executor.submit(() -> alterarAposBarreira(
                    fixture.segundaReservaId(),
                    fixture.segundoSolicitanteId(),
                    inicio,
                    fim,
                    prontas,
                    iniciar
            )));

            assertTrue(prontas.await(10, TimeUnit.SECONDS),
                    "As duas threads devem estar prontas antes da liberação.");
            iniciar.countDown();
            return List.of(
                    obterResultado(tarefas.get(0)),
                    obterResultado(tarefas.get(1))
            );
        } finally {
            iniciar.countDown();
            encerrarExecutor(executor);
        }
    }

    private ResultadoConcorrencia alterarAposBarreira(
            Long reservaId,
            Long solicitanteId,
            LocalDateTime inicio,
            LocalDateTime fim,
            CountDownLatch prontas,
            CountDownLatch iniciar
    ) throws InterruptedException, TimeoutException {
        prontas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) {
            throw new TimeoutException("A liberação das alterações concorrentes expirou.");
        }

        try {
            Reserva reserva = reservaService.alterarHorarioReserva(
                    reservaId, solicitanteId, inicio, fim
            );
            return new ResultadoConcorrencia(reserva, null);
        } catch (IllegalStateException conflito) {
            return new ResultadoConcorrencia(null, conflito);
        }
    }

    private void encerrarExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        boolean terminou;
        try {
            terminou = executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException interrompida) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw interrompida;
        }
        if (!terminou) {
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "As threads concorrentes não terminaram antes da limpeza."
                );
            }
        }
    }

    private ResultadoConcorrencia solicitarAposBarreira(
            FixtureConcorrencia fixture,
            Intervalo intervalo,
            CountDownLatch prontas,
            CountDownLatch iniciar
    ) throws InterruptedException, TimeoutException {
        return solicitarAposBarreira(
                fixture.salaId(),
                fixture.solicitanteId(),
                TipoSolicitante.PROFESSOR,
                List.of(),
                List.of(),
                prontas,
                iniciar,
                intervalo
        );
    }

    private ResultadoConcorrencia solicitarAposBarreira(
            Long salaId,
            Long solicitanteId,
            TipoSolicitante tipoSolicitante,
            List<Long> professorIds,
            List<Long> materialIds,
            CountDownLatch prontas,
            CountDownLatch iniciar
    ) throws InterruptedException, TimeoutException {
        return solicitarAposBarreira(
                salaId,
                solicitanteId,
                tipoSolicitante,
                professorIds,
                materialIds,
                prontas,
                iniciar,
                new Intervalo(INICIO, FIM)
        );
    }

    private ResultadoConcorrencia solicitarAposBarreira(
            Long salaId,
            Long solicitanteId,
            TipoSolicitante tipoSolicitante,
            List<Long> professorIds,
            List<Long> materialIds,
            CountDownLatch prontas,
            CountDownLatch iniciar,
            Intervalo intervalo
    ) throws InterruptedException, TimeoutException {
        prontas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) {
            throw new TimeoutException("A liberação das solicitações concorrentes expirou.");
        }

        try {
            Reserva reserva = reservaService.solicitarReserva(
                    salaId,
                    solicitanteId,
                    tipoSolicitante,
                    intervalo.inicio(),
                    intervalo.fim(),
                    professorIds,
                    materialIds
            );
            return new ResultadoConcorrencia(reserva, null);
        } catch (IllegalStateException conflito) {
            return new ResultadoConcorrencia(null, conflito);
        }
    }

    private ResultadoConcorrencia obterResultado(
            Future<ResultadoConcorrencia> tarefa
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return tarefa.get(20, TimeUnit.SECONDS);
    }

    private long contarReservasNoIntervalo(
            Long salaId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        return reservaRepository.contarConflitos(
                salaId,
                STATUS_QUE_OCUPAM_SALA,
                inicio,
                fim,
                null
        );
    }

    private TransactionTemplate novaTransacao() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private record FixtureConcorrencia(Long salaId, Long solicitanteId) {
    }

    private record FixtureAlteracaoConcorrente(
            Long salaId,
            Long primeiraReservaId,
            Long segundaReservaId,
            Long primeiroSolicitanteId,
            Long segundoSolicitanteId
    ) {
    }

    private record FixtureConcorrenciaRecursos(
            Long primeiraSalaId,
            Long segundaSalaId,
            Long primeiroSolicitanteId,
            Long segundoSolicitanteId,
            Long professorId,
            Long materialId
    ) {
    }

    private record FixtureAprovacaoConcorrente(
            Long salaId,
            Long primeiraReservaId,
            Long segundaReservaId,
            Long primeiroSolicitanteId,
            Long segundoSolicitanteId,
            Long responsavelId
    ) {
    }

    private record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
    }

    private record ResultadoConcorrencia(
            Reserva reserva,
            IllegalStateException conflito
    ) {
    }

    private record ResultadoAprovacaoConcorrente(
            Reserva reserva,
            IllegalStateException conflito
    ) {
    }

    private static Stream<Arguments> statusDeOcupacao() {
        return Stream.of(
                Arguments.of(StatusReserva.SOLICITADA, true),
                Arguments.of(StatusReserva.APROVADA, true),
                Arguments.of(StatusReserva.EM_USO, true),
                Arguments.of(StatusReserva.CONCLUIDA, true),
                Arguments.of(StatusReserva.NAO_COMPARECEU, true),
                Arguments.of(StatusReserva.REJEITADA, false),
                Arguments.of(StatusReserva.CANCELADA, false)
        );
    }

    private Sala salvarSala() {
        return salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );
    }

    private Solicitante salvarSolicitante() {
        return salvarSolicitante(TipoSolicitante.PROFESSOR);
    }

    private Solicitante salvarSolicitante(TipoSolicitante tipoSolicitante) {
        String identificador = UUID.randomUUID().toString();
        return solicitanteRepository.saveAndFlush(new Solicitante(
                null,
                "Solicitante de integração",
                identificador + "@teste.local",
                "senha-de-teste",
                tipoSolicitante,
                LocalDate.now(),
                LocalDate.now()
        ));
    }

    private Reserva salvarReserva(
            Sala sala,
            Solicitante solicitante,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva status
    ) {
        return reservaRepository.saveAndFlush(new Reserva(
                null, sala, solicitante, null, inicio, fim, status, null, null
        ));
    }
}
