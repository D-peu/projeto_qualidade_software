package br.com.projeto_qualidade_software.projeto.api.dto;

import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DecisaoReservaRequest(
        @NotNull @Positive Long usuarioId,
        @NotNull TipoAprovadorReserva tipoAprovador
) {
}