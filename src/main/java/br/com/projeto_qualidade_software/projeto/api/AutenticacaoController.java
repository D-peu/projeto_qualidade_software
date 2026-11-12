package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.security.AutenticacaoInterceptor;
import br.com.projeto_qualidade_software.projeto.security.AutenticacaoService;
import br.com.projeto_qualidade_software.projeto.security.CredenciaisInvalidasException;
import br.com.projeto_qualidade_software.projeto.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(AutenticacaoInterceptor.CSRF_SESSAO);
        if (token == null) {
            token = novoTokenCsrf();
            session.setAttribute(AutenticacaoInterceptor.CSRF_SESSAO, token);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request
    ) {
        UsuarioAutenticado usuario;
        try {
            usuario = autenticacaoService.autenticar(requestBody.email(), requestBody.senha());
        } catch (CredenciaisInvalidasException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .body(new ErroResponse(exception.getMessage()));
        }

        HttpSession sessaoAnterior = request.getSession(false);
        if (sessaoAnterior != null) {
            sessaoAnterior.invalidate();
        }
        HttpSession sessao = request.getSession(true);
        sessao.setMaxInactiveInterval(30 * 60);
        sessao.setAttribute(AutenticacaoInterceptor.USUARIO_SESSAO, usuario);
        String csrfToken = novoTokenCsrf();
        sessao.setAttribute(AutenticacaoInterceptor.CSRF_SESSAO, csrfToken);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AuthResponse.de(usuario, csrfToken));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UsuarioAutenticado usuario = (UsuarioAutenticado) session.getAttribute(
                AutenticacaoInterceptor.USUARIO_SESSAO
        );
        String csrfToken = (String) session.getAttribute(AutenticacaoInterceptor.CSRF_SESSAO);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AuthResponse.de(usuario, csrfToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private String novoTokenCsrf() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha
    ) {
    }

    public record CsrfResponse(String csrfToken) {
    }

    public record AuthResponse(
            Long id,
            String nome,
            String email,
            String perfil,
            String tipoSolicitante,
            String csrfToken
    ) {
        static AuthResponse de(UsuarioAutenticado usuario, String csrfToken) {
            return new AuthResponse(
                    usuario.id(),
                    usuario.nome(),
                    usuario.email(),
                    usuario.perfil().name(),
                    usuario.tipoSolicitante() == null ? null : usuario.tipoSolicitante().name(),
                    csrfToken
            );
        }
    }

    public record ErroResponse(String mensagem) {
    }
}
