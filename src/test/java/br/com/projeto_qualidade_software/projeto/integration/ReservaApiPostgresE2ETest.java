package br.com.projeto_qualidade_software.projeto.integration;

import br.com.projeto_qualidade_software.projeto.api.ApiExceptionHandler;
import br.com.projeto_qualidade_software.projeto.api.dto.ReservaResponse;
import br.com.projeto_qualidade_software.projeto.api.dto.SolicitarReservaRequest;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.seed.enabled=false",
                "app.auth.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        }
)
@Import(ReservaApiPostgresE2ETest.FixedClockConfiguration.class)
@Testcontainers
class ReservaApiPostgresE2ETest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2030, 10, 25, 10, 0);
    private static final LocalDateTime INICIO =
            LocalDateTime.of(2030, 10, 26, 10, 0);
    private static final LocalDateTime FIM =
            LocalDateTime.of(2030, 10, 26, 11, 0);
    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_SALA = Set.of(
            StatusReserva.SOLICITADA,
            StatusReserva.APROVADA,
            StatusReserva.EM_USO
    );

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("reservas_e2e_test")
                    .withUsername("test")
                    .withPassword("test")
                    .waitingFor(Wait.forSuccessfulCommand(
                            "pg_isready -U test -d reservas_e2e_test"
                    ).withStartupTimeout(Duration.ofSeconds(60)));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long salaId;
    private Long solicitanteId;

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void prepararDados() {
        Sala sala = salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );
        salaId = sala.getId();

        String identificador = UUID.randomUUID().toString();
        Solicitante solicitante = solicitanteRepository.saveAndFlush(new Solicitante(
                null,
                "Solicitante E2E",
                identificador + "@teste.local",
                "senha-de-teste",
                TipoSolicitante.PROFESSOR,
                LocalDate.now(),
                LocalDate.now()
        ));
        solicitanteId = solicitante.getId();
    }

    @AfterEach
    void limparDados() {
        if (salaId != null) {
            jdbcTemplate.update(
                    "delete from auditorias_reserva where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    salaId
            );
            jdbcTemplate.update(
                    "delete from reservas_professores where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    salaId
            );
            jdbcTemplate.update(
                    "delete from reservas_materiais where reserva_id in "
                            + "(select id from reservas where sala_id = ?)",
                    salaId
            );
            jdbcTemplate.update("delete from reservas where sala_id = ?", salaId);
        }
        if (solicitanteId != null) {
            jdbcTemplate.update("delete from solicitantes where id = ?", solicitanteId);
        }
        if (salaId != null) {
            jdbcTemplate.update("delete from salas where id = ?", salaId);
        }
    }

    @Test
    void deveCriarReservaPorHttpConsultarEConfirmarPersistencia() {
        ResponseEntity<ReservaResponse> criada = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(INICIO, FIM, TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );

        assertEquals(HttpStatus.CREATED, criada.getStatusCode());
        assertNotNull(criada.getHeaders().getLocation());
        ReservaResponse corpoCriacao = criada.getBody();
        assertNotNull(corpoCriacao);
        assertNotNull(corpoCriacao.id());
        assertEquals(salaId, corpoCriacao.salaId());
        assertEquals(solicitanteId, corpoCriacao.solicitanteId());
        assertEquals(INICIO, corpoCriacao.inicio());
        assertEquals(FIM, corpoCriacao.fim());
        assertEquals(StatusReserva.SOLICITADA, corpoCriacao.status());
        assertNotNull(corpoCriacao.solicitadoEm());

        URI localizacao = criada.getHeaders().getLocation();
        assertTrue(localizacao.getPath().endsWith(
                "/api/reservas/" + corpoCriacao.id()
        ));
        ResponseEntity<ReservaResponse> recuperada =
                restTemplate.getForEntity(localizacao, ReservaResponse.class);

        assertEquals(HttpStatus.OK, recuperada.getStatusCode());
        ReservaResponse corpoRecuperado = recuperada.getBody();
        assertNotNull(corpoRecuperado);
        assertEquals(corpoCriacao.id(), corpoRecuperado.id());
        assertEquals(corpoCriacao.salaId(), corpoRecuperado.salaId());
        assertEquals(corpoCriacao.solicitanteId(), corpoRecuperado.solicitanteId());
        assertEquals(corpoCriacao.inicio(), corpoRecuperado.inicio());
        assertEquals(corpoCriacao.fim(), corpoRecuperado.fim());
        assertEquals(corpoCriacao.status(), corpoRecuperado.status());
        assertNotNull(corpoRecuperado.solicitadoEm());
        assertTrue(reservaRepository.findById(corpoCriacao.id()).isPresent());
    }

    @Test
    void deveRetornarBadRequestQuandoCampoObrigatorioDoDtoEstiverAusente() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requisicaoInvalida = Map.of(
                "salaId", salaId,
                "solicitanteId", solicitanteId,
                "tipoSolicitante", TipoSolicitante.PROFESSOR,
                "inicio", INICIO
        );

        ResponseEntity<ApiExceptionHandler.ErroResponse> resposta =
                restTemplate.postForEntity(
                        "/api/reservas",
                        new HttpEntity<>(requisicaoInvalida, headers),
                        ApiExceptionHandler.ErroResponse.class
                );

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertTrue(resposta.getBody().mensagem().contains("fim"));
        assertEquals(0L, contarReservasConflitantes());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-101–113 / RF07 / RN01 - API rejeita intervalos inválidos em criação e alteração")
    @MethodSource("intervalosInvalidosRF07")
    void deveRetornarBadRequestRF07ComMensagemParaIntervalosInvalidosNaCriacaoEAlteracao(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim,
            String mensagem
    ) {
        ResponseEntity<ApiExceptionHandler.ErroResponse> criacao =
                restTemplate.postForEntity(
                        "/api/reservas",
                        solicitarReserva(inicio, fim, TipoSolicitante.PROFESSOR),
                        ApiExceptionHandler.ErroResponse.class
                );

        assertEquals(HttpStatus.BAD_REQUEST, criacao.getStatusCode(), caso);
        assertNotNull(criacao.getBody(), caso);
        assertTrue(criacao.getBody().mensagem().contains(mensagem), caso);

        ResponseEntity<ReservaResponse> criada = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(INICIO, FIM, TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );
        assertEquals(HttpStatus.CREATED, criada.getStatusCode(), caso);
        assertNotNull(criada.getBody(), caso);

        ResponseEntity<ApiExceptionHandler.ErroResponse> alteracao =
                restTemplate.exchange(
                        "/api/reservas/{reservaId}/horario",
                        HttpMethod.PUT,
                        new HttpEntity<>(Map.of(
                                "solicitanteId", solicitanteId,
                                "inicio", inicio,
                                "fim", fim
                        )),
                        ApiExceptionHandler.ErroResponse.class,
                        criada.getBody().id()
                );

        assertEquals(HttpStatus.BAD_REQUEST, alteracao.getStatusCode(), caso);
        assertNotNull(alteracao.getBody(), caso);
        assertTrue(alteracao.getBody().mensagem().contains(mensagem), caso);
    }

    @Test
    @DisplayName("CP-116 / RF07 / RN01 - API aceita intervalo adjacente")
    void deveAceitarIntervalosAdjacentesPorHttp() {
        ResponseEntity<ReservaResponse> primeira = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(INICIO, FIM, TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );
        assertEquals(HttpStatus.CREATED, primeira.getStatusCode());

        ResponseEntity<ReservaResponse> adjacente = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(FIM, FIM.plusHours(1), TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );

        assertEquals(HttpStatus.CREATED, adjacente.getStatusCode());
        assertNotNull(adjacente.getBody());
        assertEquals(FIM, adjacente.getBody().inicio());
        assertEquals(FIM.plusHours(1), adjacente.getBody().fim());
    }

    @Test
    @DisplayName("CP-91 / RF12 - Filtra pendentes e ordena pela data de solicitacao")
    void deveListarSomenteReservasSolicitadasEmOrdemDeSolicitacao() {
        LocalDateTime solicitadaMaisAntiga = LocalDateTime.of(2030, 10, 20, 8, 0);
        LocalDateTime solicitadaIntermediaria = solicitadaMaisAntiga.plusHours(1);
        LocalDateTime solicitadaMaisRecente = solicitadaMaisAntiga.plusHours(2);
        Reserva primeira = salvarReserva(StatusReserva.SOLICITADA, solicitadaMaisAntiga);
        Reserva segunda = salvarReserva(StatusReserva.SOLICITADA, solicitadaIntermediaria);
        Reserva terceira = salvarReserva(StatusReserva.SOLICITADA, solicitadaMaisRecente);
        salvarReserva(StatusReserva.APROVADA, solicitadaMaisAntiga.minusHours(3));
        salvarReserva(StatusReserva.REJEITADA, solicitadaMaisAntiga.minusHours(2));
        salvarReserva(StatusReserva.CANCELADA, solicitadaMaisAntiga.minusHours(1));

        ResponseEntity<ReservaResponse[]> resposta =
                restTemplate.getForEntity("/api/reservas/pendentes", ReservaResponse[].class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        ReservaResponse[] reservas = resposta.getBody();
        assertNotNull(reservas);
        assertEquals(3, reservas.length);
        assertEquals(
                List.of(primeira.getId(), segunda.getId(), terceira.getId()),
                Arrays.stream(reservas).map(ReservaResponse::id).toList()
        );
        assertTrue(Arrays.stream(reservas)
                .allMatch(reserva -> reserva.status() == StatusReserva.SOLICITADA));
    }

    @Test
    @DisplayName("CP-92 / RF12 - Retorna lista vazia quando não há pendências")
    void deveRetornarListaVaziaQuandoNaoHaReservasPendentes() {
        ResponseEntity<ReservaResponse[]> resposta =
                restTemplate.getForEntity("/api/reservas/pendentes", ReservaResponse[].class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals(0, resposta.getBody().length);
    }

    @Test
    void deveRetornarConflictSemPersistirSegundaReservaConflitante() {
        ResponseEntity<ReservaResponse> primeira = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(INICIO, FIM, TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );
        assertEquals(HttpStatus.CREATED, primeira.getStatusCode());

        ResponseEntity<ApiExceptionHandler.ErroResponse> segunda =
                restTemplate.postForEntity(
                        "/api/reservas",
                        solicitarReserva(
                                INICIO.plusMinutes(30),
                                FIM.plusMinutes(30),
                                TipoSolicitante.PROFESSOR
                        ),
                        ApiExceptionHandler.ErroResponse.class
                );

        assertEquals(HttpStatus.CONFLICT, segunda.getStatusCode());
        assertNotNull(segunda.getBody());
        assertTrue(segunda.getBody().mensagem()
                .contains("Já existe uma reserva para esta sala"));
        assertEquals(
                1L,
                contarReservasConflitantes()
        );
    }

    @Test
    void deveCancelarReservaPorHttpEConsultarEstadoPersistido() {
        ResponseEntity<ReservaResponse> criada = restTemplate.postForEntity(
                "/api/reservas",
                solicitarReserva(INICIO, FIM, TipoSolicitante.PROFESSOR),
                ReservaResponse.class
        );
        assertEquals(HttpStatus.CREATED, criada.getStatusCode());
        ReservaResponse reserva = criada.getBody();
        assertNotNull(reserva);

        ResponseEntity<ReservaResponse> cancelada = restTemplate.postForEntity(
                "/api/reservas/{reservaId}/cancelamento",
                Map.of("usuarioId", solicitanteId),
                ReservaResponse.class,
                reserva.id()
        );

        assertEquals(HttpStatus.OK, cancelada.getStatusCode());
        assertNotNull(cancelada.getBody());
        assertEquals(StatusReserva.CANCELADA, cancelada.getBody().status());

        URI localizacao = criada.getHeaders().getLocation();
        assertNotNull(localizacao);
        ResponseEntity<ReservaResponse> recuperada =
                restTemplate.getForEntity(localizacao, ReservaResponse.class);
        assertEquals(HttpStatus.OK, recuperada.getStatusCode());
        assertNotNull(recuperada.getBody());
        assertEquals(StatusReserva.CANCELADA, recuperada.getBody().status());
    }

    @Test
    void deveRetornarForbiddenQuandoTipoInformadoDivergirDoCadastro() {
        ResponseEntity<ApiExceptionHandler.ErroResponse> resposta =
                restTemplate.postForEntity(
                        "/api/reservas",
                        solicitarReserva(INICIO, FIM, TipoSolicitante.COORDENADOR),
                        ApiExceptionHandler.ErroResponse.class
                );

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertTrue(resposta.getBody().mensagem()
                .contains("não corresponde ao perfil cadastrado"));
        assertEquals(0L, contarReservasConflitantes());
    }

    private long contarReservasConflitantes() {
        return reservaRepository.contarConflitos(
                salaId,
                STATUS_QUE_OCUPAM_SALA,
                INICIO,
                FIM,
                null
        );
    }

    private SolicitarReservaRequest solicitarReserva(
            LocalDateTime inicio,
            LocalDateTime fim,
            TipoSolicitante tipoSolicitante
    ) {
        return new SolicitarReservaRequest(
                salaId,
                solicitanteId,
                tipoSolicitante,
                inicio,
                fim,
                List.of(),
                List.of()
        );
    }

    private Reserva salvarReserva(StatusReserva status, LocalDateTime solicitadoEm) {
        Reserva reserva = new Reserva(
                null,
                salaRepository.findById(salaId).orElseThrow(),
                solicitanteRepository.findById(solicitanteId).orElseThrow(),
                null,
                INICIO,
                FIM,
                status,
                solicitadoEm,
                null
        );
        return reservaRepository.saveAndFlush(reserva);
    }

    private static Stream<Arguments> intervalosInvalidosRF07() {
        LocalDateTime diaFuturo = AGORA.plusDays(1);
        return Stream.of(
                Arguments.of(
                        "data passada", AGORA.minusDays(1),
                        AGORA.minusDays(1).plusHours(1), "futuro"
                ),
                Arguments.of("exatamente agora", AGORA, AGORA.plusHours(1), "futuro"),
                Arguments.of(
                        "um minuto no futuro", AGORA.plusMinutes(1),
                        AGORA.plusMinutes(1).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "início no minuto 15", diaFuturo.withMinute(15),
                        diaFuturo.withMinute(15).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "início no minuto 45", diaFuturo.withMinute(45),
                        diaFuturo.withMinute(45).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "fim no minuto 15", diaFuturo,
                        diaFuturo.plusHours(1).withMinute(15), "30 minutos"
                ),
                Arguments.of(
                        "fim no minuto 45", diaFuturo,
                        diaFuturo.plusHours(1).withMinute(45), "30 minutos"
                ),
                Arguments.of(
                        "segundos no início", diaFuturo.withSecond(1),
                        diaFuturo.plusHours(1), "segundos e nanos"
                ),
                Arguments.of(
                        "segundos no fim", diaFuturo,
                        diaFuturo.plusHours(1).withSecond(1), "segundos e nanos"
                ),
                Arguments.of(
                        "nanos no início", diaFuturo.withNano(1),
                        diaFuturo.plusHours(1), "segundos e nanos"
                ),
                Arguments.of(
                        "nanos no fim", diaFuturo,
                        diaFuturo.plusHours(1).withNano(1), "segundos e nanos"
                ),
                Arguments.of("fim igual ao início", diaFuturo, diaFuturo, "posterior"),
                Arguments.of(
                        "fim anterior ao início", diaFuturo.plusHours(1), diaFuturo,
                        "posterior"
                )
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock rf07TestClock() {
            return Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }
}
