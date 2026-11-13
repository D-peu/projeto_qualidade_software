package br.com.projeto_qualidade_software.projeto.api.dto;

import br.com.projeto_qualidade_software.projeto.entities.AlteracaoHorarioAplicada;

import java.time.Instant;
import java.time.LocalDateTime;

public record AlteracaoHorarioResponse(
        Long id,
        Long responsavelId,
        LocalDateTime inicioAnterior,
        LocalDateTime fimAnterior,
        LocalDateTime novoInicio,
        LocalDateTime novoFim,
        Instant aplicadaEm
) {
    public static AlteracaoHorarioResponse de(AlteracaoHorarioAplicada alteracao) {
        return new AlteracaoHorarioResponse(
                alteracao.getId(),
                alteracao.getResponsavel().getId(),
                alteracao.getOldInicio(),
                alteracao.getOldFim(),
                alteracao.getNewInicio(),
                alteracao.getNewFim(),
                alteracao.getAppliedAt()
        );
    }
}
