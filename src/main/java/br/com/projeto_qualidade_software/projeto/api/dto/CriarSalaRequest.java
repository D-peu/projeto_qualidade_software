package br.com.projeto_qualidade_software.projeto.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CriarSalaRequest(
        @NotNull @Min(value = 20, message = "deve ser ao menos 20")
        Integer capacidade
) {
}
