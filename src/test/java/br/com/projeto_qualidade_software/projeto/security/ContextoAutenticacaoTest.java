package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextoAutenticacaoTest {

    @AfterEach
    void limparContexto() {
        ContextoAutenticacao.limpar();
    }

    @Test
    void solicitanteNaoPodeInformarOutroIdOuTipo() {
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                7L, "Professor", "professor@teste.local",
                PerfilUsuario.SOLICITANTE, TipoSolicitante.PROFESSOR
        ));

        assertDoesNotThrow(() ->
                ContextoAutenticacao.exigirSolicitante(7L, TipoSolicitante.PROFESSOR));
        assertThrows(AcessoNegadoException.class, () ->
                ContextoAutenticacao.exigirSolicitante(8L, TipoSolicitante.PROFESSOR));
        assertThrows(AcessoNegadoException.class, () ->
                ContextoAutenticacao.exigirSolicitante(7L, TipoSolicitante.COORDENADOR));
        assertThrows(AcessoNegadoException.class, ContextoAutenticacao::exigirEquipe);
    }

    @Test
    void responsavelNaoPodeSePassarPorAdministrador() {
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                4L, "Responsável", "responsavel@teste.local",
                PerfilUsuario.RESPONSAVEL, null
        ));

        assertDoesNotThrow(() ->
                ContextoAutenticacao.exigirAprovador(4L, TipoAprovadorReserva.RESPONSAVEL));
        assertThrows(AcessoNegadoException.class, () ->
                ContextoAutenticacao.exigirAprovador(4L, TipoAprovadorReserva.ADMINISTRADOR));
        assertThrows(AcessoNegadoException.class, () ->
                ContextoAutenticacao.exigirAprovador(5L, TipoAprovadorReserva.RESPONSAVEL));
    }

    @Test
    void usuarioAutenticadoPodeSerSerializadoNaSessao() throws Exception {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                7L, "Professor", "professor@teste.local",
                PerfilUsuario.SOLICITANTE, TipoSolicitante.PROFESSOR
        );
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(usuario);
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())
        )) {
            assertEquals(usuario, input.readObject());
        }
    }
}
