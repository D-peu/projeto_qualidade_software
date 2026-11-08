package br.com.projeto_qualidade_software.projeto.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String USUARIO_SESSAO = "usuarioAutenticado";
    public static final String CSRF_SESSAO = "tokenCsrf";
    static final String CABECALHO_CSRF = "X-CSRF-Token";

    private final ObjectMapper objectMapper;

    public AutenticacaoInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (isRotaCsrf(request)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (isMutacao(request) && !validarCsrf(request, session)) {
            responderErro(response, HttpServletResponse.SC_FORBIDDEN, "Token de segurança inválido.");
            return false;
        }
        if (isRotaLogin(request)) {
            return true;
        }

        UsuarioAutenticado usuario = session == null
                ? null
                : (UsuarioAutenticado) session.getAttribute(USUARIO_SESSAO);
        if (usuario == null) {
            responderErro(response, HttpServletResponse.SC_UNAUTHORIZED, "Faça login para continuar.");
            return false;
        }

        ContextoAutenticacao.definir(usuario);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        ContextoAutenticacao.limpar();
    }

    private boolean validarCsrf(HttpServletRequest request, HttpSession session) {
        if (session == null) {
            return false;
        }
        Object valorSessao = session.getAttribute(CSRF_SESSAO);
        String valorRequisicao = request.getHeader(CABECALHO_CSRF);
        return valorSessao instanceof String token
                && valorRequisicao != null
                && MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        valorRequisicao.getBytes(StandardCharsets.UTF_8)
                );
    }

    private boolean isMutacao(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod())
                && !HttpMethod.HEAD.matches(request.getMethod())
                && !HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private boolean isRotaLogin(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/api/auth/login".equals(request.getRequestURI());
    }

    private boolean isRotaCsrf(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod())
                && "/api/auth/csrf".equals(request.getRequestURI());
    }

    private void responderErro(HttpServletResponse response, int status, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ErroResponse(mensagem));
    }

    private record ErroResponse(String mensagem) {
    }
}
