package br.com.projeto_qualidade_software.projeto.integration;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.AuditoriaReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.PasswordHasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.auth.enabled=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@Import(ReservaAutenticacaoPostgresE2ETest.FixedClockConfiguration.class)
@Testcontainers
class ReservaAutenticacaoPostgresE2ETest {

    private static final String PASSWORD = "Reserva@123";
    private static final String SENHA_HASH = new PasswordHasher().encode(PASSWORD);
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2030, 10, 25, 10, 0);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("reservas_auth_e2e_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AuditoriaReservaRepository auditoriaReservaRepository;

    private String administradorEmail;
    private String responsavelEmail;
    private String outroResponsavelEmail;
    private String solicitanteEmail;
    private Long salaId;
    private Long solicitanteId;
    private Long administradorId;
    private Long responsavelId;
    private Long outroResponsavelId;

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void prepararContasEReservaBase() {
        String sufixo = UUID.randomUUID().toString();
        administradorEmail = "admin-" + sufixo + "@teste.local";
        responsavelEmail = "responsavel-" + sufixo + "@teste.local";
        outroResponsavelEmail = "responsavel-outro-" + sufixo + "@teste.local";
        solicitanteEmail = "solicitante-" + sufixo + "@teste.local";
        LocalDate hoje = LocalDate.now();

        Administrador administrador = administradorRepository.saveAndFlush(
                new Administrador(
                        null,
                        "Administrador HTTP",
                        administradorEmail,
                        SENHA_HASH,
                        hoje,
                        hoje
                )
        );
        Responsavel responsavel = responsavelRepository.saveAndFlush(new Responsavel(
                null, "Responsável HTTP", responsavelEmail, SENHA_HASH, hoje, hoje
        ));
        Responsavel outroResponsavel = responsavelRepository.saveAndFlush(new Responsavel(
                null,
                "Outro Responsável HTTP",
                outroResponsavelEmail,
                SENHA_HASH,
                hoje,
                hoje
        ));
        Solicitante solicitante = solicitanteRepository.saveAndFlush(new Solicitante(
                null,
                "Solicitante HTTP",
                solicitanteEmail,
                SENHA_HASH,
                TipoSolicitante.COORDENADOR,
                hoje,
                hoje
        ));
        Sala sala = salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );

        administradorId = administrador.getId();
        responsavelId = responsavel.getId();
        outroResponsavelId = outroResponsavel.getId();
        solicitanteId = solicitante.getId();
        salaId = sala.getId();
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
            jdbcTemplate.update("delete from salas where id = ?", salaId);
        }
        if (solicitanteId != null) {
            jdbcTemplate.update("delete from solicitantes where id = ?", solicitanteId);
        }
        if (responsavelId != null && outroResponsavelId != null) {
            jdbcTemplate.update(
                    "delete from responsaveis where id in (?, ?)",
                    responsavelId,
                    outroResponsavelId
            );
        }
        if (administradorId != null) {
            jdbcTemplate.update("delete from administradores where id = ?", administradorId);
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-142 / CB-84 / RF02 / RF15 - Administrador opera reservas de qualquer Responsável")
    @MethodSource("operacoes")
    void administradorOperaQualquerReserva(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, outroResponsavelId);
        LoggedInSession administrador = login(administradorEmail);

        executar(operacao, reserva.getId(), administrador, "RESPONSAVEL", responsavelId)
                .andExpect(status().isOk());

        verificarEstadoEAuditoria(
                reserva.getId(),
                estadoInicial,
                estadoFinal,
                administradorId,
                "ADMINISTRADOR"
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-143 / CB-85 / RF02 / RF15 - Responsável opera sua reserva")
    @MethodSource("operacoes")
    void responsavelOperaReservaSobSuaResponsabilidade(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, responsavelId);
        LoggedInSession responsavel = login(responsavelEmail);

        executar(operacao, reserva.getId(), responsavel, "ADMINISTRADOR", administradorId)
                .andExpect(status().isOk());

        verificarEstadoEAuditoria(
                reserva.getId(),
                estadoInicial,
                estadoFinal,
                responsavelId,
                "RESPONSAVEL"
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-144 / CB-86 / RF02 / RF15 - Responsável não opera reserva alheia")
    @MethodSource("operacoes")
    void responsavelNaoOperaReservaDeOutroResponsavel(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, outroResponsavelId);
        LoggedInSession responsavel = login(responsavelEmail);

        executar(operacao, reserva.getId(), responsavel, "ADMINISTRADOR", outroResponsavelId)
                .andExpect(status().isForbidden());

        verificarSemTransicao(reserva.getId(), estadoInicial);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-145 / CB-87 / RF02 / RF15 - Solicitante não executa transições operacionais")
    @MethodSource("operacoes")
    void solicitanteNaoExecutaTransicaoOperacional(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, responsavelId);
        LoggedInSession solicitante = login(solicitanteEmail);

        executar(operacao, reserva.getId(), solicitante, "ADMINISTRADOR", administradorId)
                .andExpect(status().isForbidden());

        verificarSemTransicao(reserva.getId(), estadoInicial);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-146 / CB-88 / RF01-RF02 / RF15 - Sessão anônima não opera reservas")
    @MethodSource("operacoes")
    void sessaoSemUsuarioAutenticadoNaoExecutaTransicao(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, responsavelId);
        LoggedInSession anonimo = obterSessaoAnonima();

        executar(operacao, reserva.getId(), anonimo, "ADMINISTRADOR", administradorId)
                .andExpect(status().isUnauthorized());

        verificarSemTransicao(reserva.getId(), estadoInicial);
    }

    @ParameterizedTest(name = "{0}, token ausente={5}")
    @DisplayName("CP-147/148 / CB-89 / RF02 / RF15 - CSRF ausente ou inválido bloqueia transição")
    @MethodSource("operacoesComCsrfInvalido")
    void csrfAusenteOuInvalidoNaoAlteraReserva(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal,
            boolean tokenAusente
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, responsavelId);
        LoggedInSession administrador = login(administradorEmail);

        var requisicao = post(operacao.rota(reserva.getId()))
                .session(administrador.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadQueTentaSubstituirAtor());
        if (!tokenAusente) {
            requisicao.header("X-CSRF-Token", "token-invalido");
        }

        mockMvc.perform(requisicao).andExpect(status().isForbidden());
        verificarSemTransicao(reserva.getId(), estadoInicial);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-149 / CB-90 / RF02 / RF15 - Requisição sem sessão nem CSRF é recusada")
    @MethodSource("operacoes")
    void requisicaoSemSessaoNemCsrfNaoAlteraReserva(
            Operacao operacao,
            StatusReserva estadoInicial,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva estadoFinal
    ) throws Exception {
        Reserva reserva = criarReserva(estadoInicial, inicio, fim, responsavelId);

        mockMvc.perform(post(operacao.rota(reserva.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadQueTentaSubstituirAtor()))
                .andExpect(status().isForbidden());

        verificarSemTransicao(reserva.getId(), estadoInicial);
    }

    private org.springframework.test.web.servlet.ResultActions executar(
            Operacao operacao,
            Long reservaId,
            LoggedInSession sessao,
            String perfilForjado,
            Long idForjado
    ) throws Exception {
        return mockMvc.perform(post(operacao.rota(reservaId))
                .session(sessao.session())
                .header("X-CSRF-Token", sessao.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"usuarioId":%d,"perfil":"%s"}
                        """.formatted(idForjado, perfilForjado)));
    }

    private String payloadQueTentaSubstituirAtor() {
        return """
                {"usuarioId":%d,"perfil":"ADMINISTRADOR","tipoAprovador":"ADMINISTRADOR"}
                """.formatted(administradorId);
    }

    private Reserva criarReserva(
            StatusReserva status,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long idResponsavel
    ) {
        Sala sala = salaRepository.findById(salaId).orElseThrow();
        Solicitante solicitante = solicitanteRepository.findById(solicitanteId).orElseThrow();
        Responsavel responsavel =
                responsavelRepository.findById(idResponsavel).orElseThrow();
        return reservaRepository.saveAndFlush(new Reserva(
                null,
                sala,
                solicitante,
                responsavel,
                inicio,
                fim,
                status,
                AGORA.minusDays(1),
                null
        ));
    }

    private void verificarEstadoEAuditoria(
            Long reservaId,
            StatusReserva estadoAnterior,
            StatusReserva estadoEsperado,
            Long atorEsperado,
            String perfilEsperado
    ) {
        Reserva reservaPersistida = reservaRepository.findById(reservaId).orElseThrow();
        assertEquals(estadoEsperado, reservaPersistida.getStatus());

        List<AuditoriaReserva> eventos =
                auditoriaReservaRepository.findByReservaIdOrderByDataHoraAsc(reservaId);
        assertEquals(1, eventos.size());
        assertEquals(estadoAnterior, eventos.getFirst().getEstadoAnterior());
        assertEquals(estadoEsperado, eventos.getFirst().getNovoEstado());
        assertEquals(atorEsperado, eventos.getFirst().getUsuarioId());
        assertEquals(perfilEsperado, eventos.getFirst().getPerfilUsuario().name());
        assertEquals(AGORA, eventos.getFirst().getDataHora());
    }

    private void verificarSemTransicao(Long reservaId, StatusReserva estadoInicial) {
        assertEquals(
                estadoInicial,
                reservaRepository.findById(reservaId).orElseThrow().getStatus()
        );
        assertTrue(
                auditoriaReservaRepository
                        .findByReservaIdOrderByDataHoraAsc(reservaId)
                        .isEmpty()
        );
    }

    private LoggedInSession login(String email) throws Exception {
        LoggedInSession anonimo = obterSessaoAnonima();
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .session(anonimo.session())
                        .header("X-CSRF-Token", anonimo.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, PASSWORD)
                        )))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) resultado.getRequest().getSession(false);
        JsonNode corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertNotNull(session);
        assertEquals(email, corpo.path("email").asText());
        return new LoggedInSession(session, corpo.path("csrfToken").asText());
    }

    private LoggedInSession obterSessaoAnonima() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session =
                (MockHttpSession) resultado.getRequest().getSession(false);
        JsonNode corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertNotNull(session);
        return new LoggedInSession(session, corpo.path("csrfToken").asText());
    }

    private static Stream<Arguments> operacoes() {
        return Stream.of(
                Arguments.of(
                        Operacao.INICIAR,
                        StatusReserva.APROVADA,
                        AGORA,
                        AGORA.plusHours(1),
                        StatusReserva.EM_USO
                ),
                Arguments.of(
                        Operacao.CONCLUIR,
                        StatusReserva.EM_USO,
                        AGORA.minusHours(1),
                        AGORA.plusHours(1),
                        StatusReserva.CONCLUIDA
                ),
                Arguments.of(
                        Operacao.NAO_COMPARECEU,
                        StatusReserva.APROVADA,
                        AGORA.minusHours(1),
                        AGORA,
                        StatusReserva.NAO_COMPARECEU
                )
        );
    }

    private static Stream<Arguments> operacoesComCsrfInvalido() {
        return operacoes().flatMap(operacao -> Stream.of(
                Arguments.of(
                        operacao.get()[0],
                        operacao.get()[1],
                        operacao.get()[2],
                        operacao.get()[3],
                        operacao.get()[4],
                        true
                ),
                Arguments.of(
                        operacao.get()[0],
                        operacao.get()[1],
                        operacao.get()[2],
                        operacao.get()[3],
                        operacao.get()[4],
                        false
                )
        ));
    }

    private record LoginRequest(String email, String senha) {
    }

    private record LoggedInSession(MockHttpSession session, String csrfToken) {
    }

    private enum Operacao {
        INICIAR("/iniciar"),
        CONCLUIR("/concluir"),
        NAO_COMPARECEU("/nao-comparecimento");

        private final String sufixo;

        Operacao(String sufixo) {
            this.sufixo = sufixo;
        }

        private String rota(Long reservaId) {
            return "/api/reservas/" + reservaId + sufixo;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock reservaAuthTestClock() {
            return Clock.fixed(AGORA.atZone(ZONE_ID).toInstant(), ZONE_ID);
        }
    }
}
