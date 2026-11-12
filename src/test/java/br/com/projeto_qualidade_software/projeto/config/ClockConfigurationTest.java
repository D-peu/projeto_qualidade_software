package br.com.projeto_qualidade_software.projeto.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigurationTest {

    @Test
    @DisplayName("RF07 / RN01 - relógio de aplicação usa o fuso de São Paulo")
    void clockUsaFusoDeSaoPaulo() {
        assertEquals(
                "America/Sao_Paulo",
                new ClockConfiguration().clock().getZone().getId()
        );
    }
}
