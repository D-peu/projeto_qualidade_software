package br.com.projeto_qualidade_software.projeto.api.dto;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;

import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
        Long id,
        Long salaId,
        Long solicitanteId,
        Long responsavelId,
        Long administradorId,
        LocalDateTime inicio,
        LocalDateTime fim,
        StatusReserva status,
        LocalDateTime solicitadoEm,
        LocalDateTime aprovadoEm,
        List<Long> professorIds,
        List<Long> materialIds
) {

    public static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getSala().getId(),
                reserva.getSolicitante().getId(),
                reserva.getResponsavel() == null ? null : reserva.getResponsavel().getId(),
                reserva.getAdministrador() == null ? null : reserva.getAdministrador().getId(),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getStatus(),
                reserva.getSolicitadoEm(),
                reserva.getAprovadoEm(),
                reserva.getProfessores().stream()
                        .map(solicitante -> solicitante.getId())
                        .sorted()
                        .toList(),
                reserva.getMateriais().stream()
                        .map(material -> material.getId())
                        .sorted()
                        .toList()
        );
    }
}