package br.com.projeto_qualidade_software.projeto.services;

import org.springframework.web.client.RestClient;

public class HttpNotificacaoClient implements NotificacaoClient {

    private final RestClient restClient;
    private final String url;

    public HttpNotificacaoClient(RestClient restClient, String url) {
        this.restClient = restClient;
        this.url = url;
    }

    @Override
    public void notificar(NotificacaoReserva notificacao) {
        restClient.post()
                .uri(url)
                .body(notificacao)
                .retrieve()
                .toBodilessEntity();
    }
}
