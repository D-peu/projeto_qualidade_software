package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;

import java.io.Serial;
import java.io.Serializable;

public record UsuarioAutenticado(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        TipoSolicitante tipoSolicitante
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
