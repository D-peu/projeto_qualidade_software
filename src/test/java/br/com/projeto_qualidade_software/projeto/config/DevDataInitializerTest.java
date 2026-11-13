package br.com.projeto_qualidade_software.projeto.config;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevDataInitializerTest {

    private SalaRepository salaRepository;
    private SolicitanteRepository solicitanteRepository;
    private ResponsavelRepository responsavelRepository;
    private AdministradorRepository administradorRepository;
    private MaterialRepository materialRepository;
    private PasswordHasher passwordHasher;
    private DevDataInitializer initializer;

    @BeforeEach
    void setUp() {
        salaRepository = mock(SalaRepository.class);
        solicitanteRepository = mock(SolicitanteRepository.class);
        responsavelRepository = mock(ResponsavelRepository.class);
        administradorRepository = mock(AdministradorRepository.class);
        materialRepository = mock(MaterialRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        initializer = new DevDataInitializer(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository,
                materialRepository,
                passwordHasher
        );
        when(passwordHasher.encode(any())).thenAnswer(invocation ->
                "hash:" + invocation.<String>getArgument(0));
        when(solicitanteRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(responsavelRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(materialRepository.findByNomeIgnoreCase(any())).thenReturn(Optional.empty());
    }

    @Test
    void criaDadosDeDemonstracaoQuandoAindaNaoExistem() {
        when(salaRepository.count()).thenReturn(0L);

        initializer.run(mock(ApplicationArguments.class));

        verify(salaRepository).save(any());
        verify(solicitanteRepository, org.mockito.Mockito.times(2)).save(any(Solicitante.class));
        verify(responsavelRepository).save(any(Responsavel.class));
        verify(administradorRepository).save(any(Administrador.class));
        ArgumentCaptor<Material> materiais = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository, org.mockito.Mockito.times(2)).save(materiais.capture());
        assertTrue(materiais.getAllValues().stream().allMatch(Material::isRestrito));
        verify(passwordHasher, org.mockito.Mockito.times(4)).encode(any());
    }

    @Test
    void preservaDadosExistentesEAtualizaSomenteSenhasFicticias() {
        when(salaRepository.count()).thenReturn(1L);
        Solicitante solicitanteLegado = new Solicitante();
        solicitanteLegado.setPassword("senha-ficticia");
        when(solicitanteRepository.findByEmailIgnoreCase("professor@teste.local"))
                .thenReturn(Optional.of(solicitanteLegado));
        when(solicitanteRepository.findByEmailIgnoreCase("coordenador@teste.local"))
                .thenReturn(Optional.of(new Solicitante()));
        Responsavel responsavelLegado = new Responsavel();
        responsavelLegado.setPassword("senha-ficticia");
        when(responsavelRepository.findByEmailIgnoreCase("responsavel@teste.local"))
                .thenReturn(Optional.of(responsavelLegado));
        when(administradorRepository.findByEmailIgnoreCase("admin@teste.local"))
                .thenReturn(Optional.of(new Administrador()));
        when(materialRepository.findByNomeIgnoreCase("Projetor multimídia"))
                .thenReturn(Optional.of(new Material()));

        initializer.run(mock(ApplicationArguments.class));

        verify(salaRepository, never()).save(any());
        verify(solicitanteRepository).save(solicitanteLegado);
        verify(solicitanteRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                solicitante -> solicitante != solicitanteLegado
        ));
        verify(responsavelRepository).save(responsavelLegado);
        verify(administradorRepository, never()).save(any());
        verify(materialRepository).save(any(Material.class));
        verify(passwordHasher, org.mockito.Mockito.times(2)).encode(any());
    }
}
