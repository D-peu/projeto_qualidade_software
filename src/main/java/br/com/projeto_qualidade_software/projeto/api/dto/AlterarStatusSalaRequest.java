package br.com.projeto_qualidade_software.projeto.api.dto;

import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusSalaRequest(
        @NotNull StatusSala statusSala
) {
}
