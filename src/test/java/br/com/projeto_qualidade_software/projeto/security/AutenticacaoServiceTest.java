package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoServiceTest {

    private SolicitanteRepository solicitanteRepository;
    private ResponsavelRepository responsavelRepository;
    private AdministradorRepository administradorRepository;
    private PasswordHasher passwordHasher;
    private AutenticacaoService service;

    @BeforeEach
    void setUp() {
        solicitanteRepository = mock(SolicitanteRepository.class);
        responsavelRepository = mock(ResponsavelRepository.class);
        administradorRepository = mock(AdministradorRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        service = new AutenticacaoService(
                solicitanteRepository,
                responsavelRepository,
                administradorRepository,
                passwordHasher
        );
        when(solicitanteRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.empty());
        when(responsavelRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.empty());
    }

    @Test
    void autenticaSolicitanteNormalizandoEmail() {
        Solicitante solicitante = solicitante("senha-hash");
        when(solicitanteRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.of(solicitante));
        when(passwordHasher.matches("senha", "senha-hash")).thenReturn(true);
        when(passwordHasher.precisaAtualizar("senha-hash")).thenReturn(false);

        UsuarioAutenticado usuario = service.autenticar(" P@TESTE.LOCAL ", "senha");

        assertEquals(PerfilUsuario.SOLICITANTE, usuario.perfil());
        assertEquals(TipoSolicitante.PROFESSOR, usuario.tipoSolicitante());
        assertEquals(7L, usuario.id());
        verify(solicitanteRepository, never()).save(solicitante);
    }

    @Test
    void autenticaResponsavelEAtualizaSenhaLegada() {
        Responsavel responsavel = new Responsavel(
                8L, "Responsável", "p@teste.local", "senha-legada",
                LocalDate.now(), LocalDate.now()
        );
        when(responsavelRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.of(responsavel));
        when(passwordHasher.matches("senha", "senha-legada")).thenReturn(true);
        when(passwordHasher.precisaAtualizar("senha-legada")).thenReturn(true);
        when(passwordHasher.encode("senha")).thenReturn("senha-atualizada");

        UsuarioAutenticado usuario = service.autenticar("p@teste.local", "senha");

        assertEquals(PerfilUsuario.RESPONSAVEL, usuario.perfil());
        assertEquals("senha-atualizada", responsavel.getPassword());
        verify(responsavelRepository).save(responsavel);
    }

    @Test
    void autenticaAdministrador() {
        Administrador administrador = new Administrador(
                9L, "Admin", "p@teste.local", "hash",
                LocalDate.now(), LocalDate.now()
        );
        when(administradorRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.of(administrador));
        when(passwordHasher.matches("senha", "hash")).thenReturn(true);
        when(passwordHasher.precisaAtualizar("hash")).thenReturn(false);

        UsuarioAutenticado usuario = service.autenticar("p@teste.local", "senha");

        assertEquals(PerfilUsuario.ADMINISTRADOR, usuario.perfil());
        assertEquals(null, usuario.tipoSolicitante());
    }

    @Test
    void rejeitaCredenciaisNulasAusentesAmbiguasOuInvalidas() {
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar(null, "senha"));
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("p@teste.local", null));
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("p@teste.local", "senha"));

        Solicitante solicitante = solicitante("hash");
        when(solicitanteRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.of(solicitante));
        when(responsavelRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.of(new Responsavel()));
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("p@teste.local", "senha"));

        when(responsavelRepository.findByEmailIgnoreCase("p@teste.local"))
                .thenReturn(Optional.empty());
        when(passwordHasher.matches("senha", "hash")).thenReturn(false);
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("p@teste.local", "senha"));
    }

    private Solicitante solicitante(String password) {
        return new Solicitante(
                7L, "Professor", "p@teste.local", password,
                TipoSolicitante.PROFESSOR, LocalDate.now(), LocalDate.now()
        );
    }
}
