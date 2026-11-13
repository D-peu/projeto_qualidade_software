package br.com.projeto_qualidade_software.projeto.api.dto;

import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;

import java.time.LocalDateTime;

public record AuditoriaReservaResponse(
        Long id,
        StatusReserva estadoAnterior,
        StatusReserva novoEstado,
        Long usuarioId,
        PerfilUsuario perfilUsuario,
        LocalDateTime dataHora
) {
    public static AuditoriaReservaResponse de(AuditoriaReserva auditoria) {
        return new AuditoriaReservaResponse(
                auditoria.getId(),
                auditoria.getEstadoAnterior(),
                auditoria.getNovoEstado(),
                auditoria.getUsuarioId(),
                auditoria.getPerfilUsuario(),
                auditoria.getDataHora()
        );
    }
}
