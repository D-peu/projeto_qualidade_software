package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
