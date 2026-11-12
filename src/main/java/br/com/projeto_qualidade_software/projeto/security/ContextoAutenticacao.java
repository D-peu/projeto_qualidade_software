package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;

public final class ContextoAutenticacao {

    private static final ThreadLocal<UsuarioAutenticado> USUARIO_ATUAL = new ThreadLocal<>();

    private ContextoAutenticacao() {
    }

    public static void definir(UsuarioAutenticado usuario) {
        USUARIO_ATUAL.set(usuario);
    }

    public static UsuarioAutenticado usuarioAtual() {
        return USUARIO_ATUAL.get();
    }

    public static void limpar() {
        USUARIO_ATUAL.remove();
    }

    public static void exigirSolicitante(Long solicitanteId, TipoSolicitante tipoSolicitante) {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario == null) {
            return;
        }
        if (usuario.perfil() != PerfilUsuario.SOLICITANTE
                || !usuario.id().equals(solicitanteId)
                || usuario.tipoSolicitante() != tipoSolicitante) {
            throw new AcessoNegadoException("A conta autenticada não pode executar esta operação.");
        }
    }

    public static void exigirSolicitante(Long solicitanteId) {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario == null) {
            return;
        }
        if (usuario.perfil() != PerfilUsuario.SOLICITANTE
                || !usuario.id().equals(solicitanteId)) {
            throw new AcessoNegadoException("A conta autenticada não pode executar esta operação.");
        }
    }

    public static void exigirAprovador(Long usuarioId, TipoAprovadorReserva tipoAprovador) {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario == null) {
            return;
        }
        PerfilUsuario perfilEsperado = tipoAprovador == TipoAprovadorReserva.RESPONSAVEL
                ? PerfilUsuario.RESPONSAVEL
                : PerfilUsuario.ADMINISTRADOR;
        if (usuario.perfil() != perfilEsperado || !usuario.id().equals(usuarioId)) {
            throw new AcessoNegadoException("A conta autenticada não pode executar esta operação.");
        }
    }

    public static void exigirEquipe() {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario != null && usuario.perfil() == PerfilUsuario.SOLICITANTE) {
            throw new AcessoNegadoException("Somente Responsável ou Administrador pode consultar esta fila.");
        }
    }

    public static void exigirAdministrador() {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario == null || usuario.perfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new AcessoNegadoException("Somente Administrador pode cadastrar salas.");
        }
    }

    public static void exigirAcessoReserva(Reserva reserva) {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario != null
                && usuario.perfil() == PerfilUsuario.SOLICITANTE
                && !usuario.id().equals(reserva.getSolicitante().getId())) {
            throw new AcessoNegadoException("A conta autenticada não pode consultar esta reserva.");
        }
    }

    public static void exigirAcessoListaSolicitante(Long solicitanteId) {
        UsuarioAutenticado usuario = usuarioAtual();
        if (usuario != null
                && usuario.perfil() == PerfilUsuario.SOLICITANTE
                && !usuario.id().equals(solicitanteId)) {
            throw new AcessoNegadoException("A conta autenticada não pode consultar estas reservas.");
        }
    }
}
