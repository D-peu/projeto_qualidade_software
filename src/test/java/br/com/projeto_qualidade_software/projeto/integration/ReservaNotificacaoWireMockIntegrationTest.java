package br.com.projeto_qualidade_software.projeto.integration;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.services.ReservaService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.auth.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "notificacao.timeout=1s"
})
class ReservaNotificacaoWireMockIntegrationTest {

    private static final String NOTIFICATION_PATH = "/notifications/reservas";
    private static final LocalDateTime INICIO = LocalDateTime.of(2040, 1, 2, 10, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2040, 1, 2, 11, 0);
    private static final WireMockServer WIREMOCK =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        WIREMOCK.start();
    }

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private ReservaService reservaService;

    @DynamicPropertySource
    static void configureNotificationUrl(DynamicPropertyRegistry registry) {
        registry.add("notificacao.url", () -> WIREMOCK.baseUrl() + NOTIFICATION_PATH);
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @BeforeEach
    void resetWireMock() {
        WIREMOCK.resetAll();
    }

    @Test
    @DisplayName("RF11 / WireMock - Aprovacao notifica com o corpo da reserva")
    void approvalSendsExpectedNotificationBody() {
        WIREMOCK.stubFor(post(urlEqualTo(NOTIFICATION_PATH))
                .willReturn(aResponse().withStatus(200)));
        Fixture fixture = createPendingReservation();

        Reserva approved = reservaService.aprovarReserva(
                fixture.reservaId(),
                fixture.responsavelId(),
                TipoAprovadorReserva.RESPONSAVEL
        );

        assertEquals(StatusReserva.APROVADA, approved.getStatus());
        assertEquals(
                StatusReserva.APROVADA,
                reservaRepository.findById(fixture.reservaId()).orElseThrow().getStatus()
        );
        WIREMOCK.verify(postRequestedFor(urlEqualTo(NOTIFICATION_PATH))
                .withRequestBody(equalToJson("""
                        {
                          "reservaId": %d,
                          "salaId": %d,
                          "solicitanteId": %d,
                          "status": "APROVADA",
                          "inicio": "2040-01-02T10:00:00",
                          "fim": "2040-01-02T11:00:00"
                        }
                        """.formatted(
                        fixture.reservaId(),
                        fixture.salaId(),
                        fixture.solicitanteId()
                ))));
    }

    @Test
    @DisplayName("RF11 / WireMock - Falha HTTP nao desfaz rejeicao")
    void serverErrorDoesNotUndoRejection() {
        WIREMOCK.stubFor(post(urlEqualTo(NOTIFICATION_PATH))
                .willReturn(aResponse().withStatus(500)));
        Fixture fixture = createPendingReservation();

        Reserva rejected = reservaService.rejeitarReserva(
                fixture.reservaId(),
                fixture.responsavelId(),
                TipoAprovadorReserva.RESPONSAVEL
        );

        assertEquals(StatusReserva.REJEITADA, rejected.getStatus());
        assertEquals(
                StatusReserva.REJEITADA,
                reservaRepository.findById(fixture.reservaId()).orElseThrow().getStatus()
        );
        WIREMOCK.verify(postRequestedFor(urlEqualTo(NOTIFICATION_PATH)));
    }

    @Test
    @DisplayName("RF11 / WireMock - Timeout nao desfaz cancelamento")
    void notificationTimeoutDoesNotUndoCancellation() {
        WIREMOCK.stubFor(post(urlEqualTo(NOTIFICATION_PATH))
                .willReturn(aResponse().withStatus(200).withFixedDelay(1500)));
        Fixture fixture = createPendingReservation();

        Reserva cancelled = reservaService.cancelarReserva(
                fixture.reservaId(),
                fixture.solicitanteId()
        );

        assertEquals(StatusReserva.CANCELADA, cancelled.getStatus());
        assertEquals(
                StatusReserva.CANCELADA,
                reservaRepository.findById(fixture.reservaId()).orElseThrow().getStatus()
        );
        WIREMOCK.verify(postRequestedFor(urlEqualTo(NOTIFICATION_PATH)));
    }

    private Fixture createPendingReservation() {
        Sala sala = salaRepository.saveAndFlush(
                new Sala(null, 30, StatusSala.DISPONIVEL)
        );
        String identity = UUID.randomUUID().toString();
        Solicitante solicitante = solicitanteRepository.saveAndFlush(new Solicitante(
                null,
                "Solicitante WireMock",
                identity + "@teste.local",
                "senha-de-teste",
                TipoSolicitante.PROFESSOR,
                LocalDate.now(),
                LocalDate.now()
        ));
        Responsavel responsavel = responsavelRepository.saveAndFlush(new Responsavel(
                null,
                "Responsavel WireMock",
                "responsavel-" + identity + "@teste.local",
                "senha-de-teste",
                null,
                null
        ));
        Reserva reserva = reservaRepository.saveAndFlush(new Reserva(
                null,
                sala,
                solicitante,
                null,
                INICIO,
                FIM,
                StatusReserva.SOLICITADA,
                LocalDateTime.now(),
                null
        ));
        return new Fixture(reserva.getId(), sala.getId(), solicitante.getId(), responsavel.getId());
    }

    private record Fixture(Long reservaId, Long salaId, Long solicitanteId, Long responsavelId) {
    }
}
