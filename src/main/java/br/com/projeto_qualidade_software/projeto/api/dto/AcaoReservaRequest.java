package br.com.projeto_qualidade_software.projeto.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AcaoReservaRequest(
        @NotNull @Positive Long usuarioId
) {
}