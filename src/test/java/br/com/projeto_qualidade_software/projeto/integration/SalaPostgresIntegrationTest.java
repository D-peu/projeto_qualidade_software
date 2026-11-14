package br.com.projeto_qualidade_software.projeto.integration;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.PasswordHasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class SalaPostgresIntegrationTest {

    private static final String PASSWORD = "Admin@123";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("salas_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    private String adminEmail;
    private String responsibleEmail;
    private String applicantEmail;

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void prepararContas() {
        String sufixo = UUID.randomUUID().toString();
        adminEmail = "admin-" + sufixo + "@teste.local";
        responsibleEmail = "responsavel-" + sufixo + "@teste.local";
        applicantEmail = "solicitante-" + sufixo + "@teste.local";
        String senhaHash = passwordHasher.encode(PASSWORD);
        LocalDate hoje = LocalDate.now();

        administradorRepository.saveAndFlush(new Administrador(
                null, "Administrador de teste", adminEmail, senhaHash, hoje, hoje
        ));
        responsavelRepository.saveAndFlush(new Responsavel(
                null, "Responsável de teste", responsibleEmail, senhaHash, hoje, hoje
        ));
        solicitanteRepository.saveAndFlush(new Solicitante(
                null, "Solicitante de teste", applicantEmail, senhaHash,
                TipoSolicitante.PROFESSOR, hoje, hoje
        ));
    }

    @AfterEach
    void limparDados() {
        salaRepository.deleteAll();
        solicitanteRepository.deleteAll();
        responsavelRepository.deleteAll();
        administradorRepository.deleteAll();
    }

    @Test
    @DisplayName("CP-120 / CB-69 / RF03 / RF02 - Cria sala, persiste identidade gerada e relê pelo catálogo")
    void administradorCriaSalaPersistidaEVisivelNoCatalogo() throws Exception {
        Sala salaExistente = salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );
        LoggedInSession admin = login(adminEmail);

        MvcResult criada = mockMvc.perform(post("/api/salas")
                        .session(admin.session())
                        .header("X-CSRF-Token", admin.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capacidade":20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacidade").value(20))
                .andExpect(jsonPath("$.statusSala").value("DISPONIVEL"))
                .andReturn();

        JsonNode corpo = objectMapper.readTree(criada.getResponse().getContentAsString());
        assertTrue(corpo.path("id").isIntegralNumber());
        long salaId = corpo.path("id").asLong();
        assertNotEquals(salaExistente.getId(), salaId);

        Sala persistida = salaRepository.findById(salaId).orElseThrow();
        assertEquals(20, persistida.getCapacidade());
        assertEquals(StatusSala.DISPONIVEL, persistida.getStatusSala());

        MvcResult catalogo = mockMvc.perform(get("/api/salas").session(admin.session()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode salas = objectMapper.readTree(catalogo.getResponse().getContentAsString());
        boolean salaRetornada = StreamSupport.stream(salas.spliterator(), false)
                .anyMatch(sala -> sala.path("id").asLong() == salaId
                        && sala.path("capacidade").asInt() == 20
                        && "DISPONIVEL".equals(sala.path("statusSala").asText()));
        assertTrue(salaRetornada);
    }

    @Test
    @DisplayName("CP-121 / CB-70 / RF03 / INV-18 - Capacidade inválida não cria sala")
    void capacidadeInvalidaNaoPersisteSala() throws Exception {
        LoggedInSession admin = login(adminEmail);
        long totalInicial = salaRepository.count();

        postarSala(admin, "{\"capacidade\":19}")
                .andExpect(status().isBadRequest());
        postarSala(admin, "{\"capacidade\":null}")
                .andExpect(status().isBadRequest());

        assertEquals(totalInicial, salaRepository.count());
    }

    @Test
    @DisplayName("CP-122 / RF02 / INV-08 - Solicitante e Responsável não podem criar salas")
    void bloqueiaCriacaoPorPerfisSemPermissao() throws Exception {
        LoggedInSession solicitante = login(applicantEmail);
        LoggedInSession responsavel = login(responsibleEmail);
        long totalInicial = salaRepository.count();

        postarSala(solicitante, "{\"capacidade\":20}")
                .andExpect(status().isForbidden());
        postarSala(responsavel, "{\"capacidade\":20}")
                .andExpect(status().isForbidden());

        assertEquals(totalInicial, salaRepository.count());
    }

    @Test
    @DisplayName("CP-123 / RF01 / RF02 / INV-08 - Criação sem autenticação é rejeitada")
    void bloqueiaCriacaoSemSessaoAutenticada() throws Exception {
        LoggedInSession anonimo = obterSessaoAnonima();
        long totalInicial = salaRepository.count();

        postarSala(anonimo, "{\"capacidade\":20}")
                .andExpect(status().isUnauthorized());

        assertEquals(totalInicial, salaRepository.count());
    }

    @Test
    @DisplayName("CP-150/151 / RF04 / RF05 / RF11 - Atualiza estado e filtra catálogo")
    void administradorAlteraEstadoEFiltraCatalogoDeSalas() throws Exception {
        Sala salaDisponivel = salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );
        salaRepository.saveAndFlush(new Sala(null, 40, StatusSala.DISPONIVEL));
        LoggedInSession admin = login(adminEmail);

        mockMvc.perform(put("/api/salas/{salaId}/status", salaDisponivel.getId())
                        .session(admin.session())
                        .header("X-CSRF-Token", admin.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statusSala":"EM_MANUTENCAO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusSala").value("EM_MANUTENCAO"));

        mockMvc.perform(get("/api/salas")
                        .session(admin.session())
                        .param("status", "EM_MANUTENCAO")
                        .param("capacidadeMinima", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(salaDisponivel.getId()));

        assertEquals(
                StatusSala.EM_MANUTENCAO,
                salaRepository.findById(salaDisponivel.getId()).orElseThrow().getStatusSala()
        );
    }

    @Test
    @DisplayName("CP-152 / RF02 / RF04 / RF11 - Somente Administrador altera estado de sala")
    void bloqueiaAlteracaoDeEstadoPorOutrosPerfisOuSemSessao() throws Exception {
        Sala sala = salaRepository.saveAndFlush(new Sala(null, 30, StatusSala.DISPONIVEL));
        LoggedInSession solicitante = login(applicantEmail);
        LoggedInSession responsavel = login(responsibleEmail);
        LoggedInSession anonimo = obterSessaoAnonima();

        alterarStatus(sala.getId(), solicitante).andExpect(status().isForbidden());
        alterarStatus(sala.getId(), responsavel).andExpect(status().isForbidden());
        alterarStatus(sala.getId(), anonimo).andExpect(status().isUnauthorized());

        assertEquals(
                StatusSala.DISPONIVEL,
                salaRepository.findById(sala.getId()).orElseThrow().getStatusSala()
        );
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(
            Long salaId,
            LoggedInSession session
    ) throws Exception {
        return mockMvc.perform(put("/api/salas/{salaId}/status", salaId)
                .session(session.session())
                .header("X-CSRF-Token", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"statusSala":"EM_MANUTENCAO"}
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions postarSala(
            LoggedInSession session,
            String body
    ) throws Exception {
        return mockMvc.perform(post("/api/salas")
                .session(session.session())
                .header("X-CSRF-Token", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
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
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
        String csrfToken = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .path("csrfToken")
                .asText();
        return new LoggedInSession(session, csrfToken);
    }

    private LoggedInSession obterSessaoAnonima() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
        String csrfToken = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .path("csrfToken")
                .asText();
        assertNotNull(session);
        return new LoggedInSession(session, csrfToken);
    }

    private record LoginRequest(String email, String senha) {
    }

    private record LoggedInSession(MockHttpSession session, String csrfToken) {
    }
}
