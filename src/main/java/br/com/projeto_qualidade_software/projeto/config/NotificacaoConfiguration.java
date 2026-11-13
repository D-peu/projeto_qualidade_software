package br.com.projeto_qualidade_software.projeto.config;

import br.com.projeto_qualidade_software.projeto.services.HttpNotificacaoClient;
import br.com.projeto_qualidade_software.projeto.services.NotificacaoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class NotificacaoConfiguration {

    @Bean
    NotificacaoClient notificacaoClient(
            RestClient.Builder builder,
            @Value("${notificacao.url:http://localhost:8089/notificacoes/reservas}") String url,
            @Value("${notificacao.timeout:500ms}") Duration timeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        RestClient restClient = builder
                .requestFactory(requestFactory)
                .build();
        return new HttpNotificacaoClient(restClient, url);
    }
}
