package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoInterceptor;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoService;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoWebConfig;
import br.com.projeto_qualidade_software.projeto.services.ReservaService;
import br.com.projeto_qualidade_software.projeto.services.SalaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.context.annotation.Import;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                AutenticacaoController.class,
                CatalogoRecursosController.class,
                ReservaController.class,
                SalaController.class
        },
        properties = "app.auth.enabled=true"
)
@Import(AutenticacaoWebConfig.class)
class AutenticacaoRotasControllerTest {

    private static final String TOKEN_CSRF = "csrf-anonimo-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private MaterialRepository materialRepository;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private SalaService salaService;

    @MockitoBean
    private SolicitanteRepository solicitanteRepository;

    @ParameterizedTest(name = "{0}")
    @MethodSource("rotasGetProtegidas")
    @DisplayName("CP-160 / RF01 / RF02 / INV-08 - Todas as rotas GET da API exigem sessão")
    void getApiSemSessaoRetorna401(String rota) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(rota))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("rotasMutaveisProtegidas")
    @DisplayName("CP-161 / RF01 / RF02 / INV-08 - Rotas mutáveis exigem sessão autenticada")
    void mutacaoComCsrfMasSemLoginRetorna401(HttpMethod metodo, String rota)
            throws Exception {
        MockHttpSession sessaoAnonima = new MockHttpSession();
        sessaoAnonima.setAttribute(AutenticacaoInterceptor.CSRF_SESSAO, TOKEN_CSRF);

        MockHttpServletRequestBuilder requisicao =
                MockMvcRequestBuilders.request(metodo, rota)
                        .session(sessaoAnonima)
                        .header("X-CSRF-Token", TOKEN_CSRF);

        mockMvc.perform(requisicao).andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> rotasGetProtegidas() {
        return Stream.of(
                Arguments.of("/api/auth/me"),
                Arguments.of("/api/salas"),
                Arguments.of("/api/professores"),
                Arguments.of("/api/materiais"),
                Arguments.of("/api/reservas/1"),
                Arguments.of("/api/reservas/1/auditoria"),
                Arguments.of("/api/reservas/1/historico-horario"),
                Arguments.of("/api/reservas/solicitante/1"),
                Arguments.of("/api/reservas?status=SOLICITADA"),
                Arguments.of("/api/reservas/pendentes")
        );
    }

    private static Stream<Arguments> rotasMutaveisProtegidas() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/auth/logout"),
                Arguments.of(HttpMethod.POST, "/api/reservas"),
                Arguments.of(HttpMethod.PUT, "/api/reservas/1/horario"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/cancelamento"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/aprovacao"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/rejeicao"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/iniciar"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/concluir"),
                Arguments.of(HttpMethod.POST, "/api/reservas/1/nao-comparecimento"),
                Arguments.of(HttpMethod.POST, "/api/salas"),
                Arguments.of(HttpMethod.PUT, "/api/salas/1/status")
        );
    }
}
