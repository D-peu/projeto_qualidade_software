package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;

public record UsuarioAutenticado(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        TipoSolicitante tipoSolicitante
) {
}
