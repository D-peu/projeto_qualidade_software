package br.com.projeto_qualidade_software.projeto.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AlterarHorarioReservaRequest(
        @NotNull @Positive Long solicitanteId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim
) {
}