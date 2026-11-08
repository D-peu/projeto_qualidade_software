package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoService;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoWebConfig;
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;
import br.com.projeto_qualidade_software.projeto.security.UsuarioAutenticado;
import br.com.projeto_qualidade_software.projeto.services.SalaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {AutenticacaoController.class, SalaController.class},
        properties = "app.auth.enabled=true"
)
@Import(AutenticacaoWebConfig.class)
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private SalaService salaService;

    @Test
    void loginCriaSessaoEPermiteRestaurarUsuarioAutenticado() throws Exception {
        when(autenticacaoService.autenticar("professor@teste.local", "Professor@123"))
                .thenReturn(new UsuarioAutenticado(
                        7L,
                        "Professor Teste",
                        "professor@teste.local",
                        PerfilUsuario.SOLICITANTE,
                        TipoSolicitante.PROFESSOR
                ));

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andReturn();
        MockHttpSession csrfSession = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String csrfToken = csrfResult.getResponse().getContentAsString()
                .replaceAll(".*\"csrfToken\":\"([^\"]+)\".*", "$1");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(csrfSession)
                        .header("X-CSRF-Token", csrfToken)
                        .contentType("application/json")
                        .content("""
                                {"email":"professor@teste.local","senha":"Professor@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andExpect(jsonPath("$.tipoSolicitante").value("PROFESSOR"))
                .andReturn();
        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        String authenticatedCsrf = loginResult.getResponse().getContentAsString()
                .replaceAll(".*\"csrfToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("professor@teste.local"))
                .andExpect(jsonPath("$.csrfToken").value(authenticatedCsrf));

        when(salaService.listarSalas()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/salas").session(authenticatedSession))
                .andExpect(status().isOk());
    }

    @Test
    void bloqueiaApiSemSessaoEOperacaoComTokenCsrfInvalido() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .header("X-CSRF-Token", "invalid")
                        .contentType("application/json")
                        .content("""
                                {"email":"professor@teste.local","senha":"Professor@123"}
                                """))
                .andExpect(status().isForbidden());
    }
}
