package br.com.projeto_qualidade_software.projeto.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AutenticacaoWebConfig implements WebMvcConfigurer {

    private final boolean autenticacaoHabilitada;
    private final AutenticacaoInterceptor autenticacaoInterceptor;

    public AutenticacaoWebConfig(
            ObjectMapper objectMapper,
            @Value("${app.auth.enabled:true}") boolean autenticacaoHabilitada
    ) {
        this.autenticacaoHabilitada = autenticacaoHabilitada;
        this.autenticacaoInterceptor = new AutenticacaoInterceptor(objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (autenticacaoHabilitada) {
            registry.addInterceptor(autenticacaoInterceptor)
                    .addPathPatterns("/api/**");
        }
    }
}
