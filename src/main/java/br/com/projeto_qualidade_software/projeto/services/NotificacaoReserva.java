package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;

import java.time.LocalDateTime;

public record NotificacaoReserva(
        Long reservaId,
        Long salaId,
        Long solicitanteId,
        StatusReserva status,
        LocalDateTime inicio,
        LocalDateTime fim
) {
}
