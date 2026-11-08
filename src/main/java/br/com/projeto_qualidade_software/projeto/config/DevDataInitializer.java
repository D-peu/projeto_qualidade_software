package br.com.projeto_qualidade_software.projeto.config;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.PasswordHasher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Cria dados sinteticos para teste manual. So roda quando app.seed.enabled=true
 * e apenas se ainda nao existir nenhuma sala.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataInitializer implements ApplicationRunner {

    private final SalaRepository salaRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AdministradorRepository administradorRepository;
    private final MaterialRepository materialRepository;
    private final PasswordHasher passwordHasher;

    public DevDataInitializer(
            SalaRepository salaRepository,
            SolicitanteRepository solicitanteRepository,
            ResponsavelRepository responsavelRepository,
            AdministradorRepository administradorRepository,
            MaterialRepository materialRepository,
            PasswordHasher passwordHasher
    ) {
        this.salaRepository = salaRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.responsavelRepository = responsavelRepository;
        this.administradorRepository = administradorRepository;
        this.materialRepository = materialRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate hoje = LocalDate.now();
        if (salaRepository.count() == 0) {
            salaRepository.save(new Sala(null, 30, StatusSala.DISPONIVEL));
        }

        criarSolicitanteSeAusente(
                "Professor Teste", "professor@teste.local", "Professor@123",
                TipoSolicitante.PROFESSOR, hoje
        );
        criarSolicitanteSeAusente(
                "Coordenador Teste", "coordenador@teste.local", "Coordenador@123",
                TipoSolicitante.COORDENADOR, hoje
        );
        criarResponsavelSeAusente(hoje);
        criarAdministradorSeAusente(hoje);
        criarMaterialSeAusente("Projetor multimídia");
        criarMaterialSeAusente("Notebook");
    }

    private void criarSolicitanteSeAusente(
            String nome,
            String email,
            String senha,
            TipoSolicitante tipo,
            LocalDate hoje
    ) {
        Solicitante existente = solicitanteRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existente == null) {
            solicitanteRepository.save(new Solicitante(
                    null, nome, email, passwordHasher.encode(senha), tipo, hoje, hoje
            ));
        } else if ("senha-ficticia".equals(existente.getPassword())) {
            existente.setPassword(passwordHasher.encode(senha));
            solicitanteRepository.save(existente);
        }
    }

    private void criarResponsavelSeAusente(LocalDate hoje) {
        Responsavel existente = responsavelRepository
                .findByEmailIgnoreCase("responsavel@teste.local")
                .orElse(null);
        if (existente == null) {
            responsavelRepository.save(new Responsavel(
                    null, "Responsavel Teste", "responsavel@teste.local",
                    passwordHasher.encode("Responsavel@123"), hoje, hoje
            ));
        } else if ("senha-ficticia".equals(existente.getPassword())) {
            existente.setPassword(passwordHasher.encode("Responsavel@123"));
            responsavelRepository.save(existente);
        }
    }

    private void criarAdministradorSeAusente(LocalDate hoje) {
        Administrador existente = administradorRepository
                .findByEmailIgnoreCase("admin@teste.local")
                .orElse(null);
        if (existente == null) {
            administradorRepository.save(new Administrador(
                    null, "Administrador Teste", "admin@teste.local",
                    passwordHasher.encode("Admin@123"), hoje, hoje
            ));
        } else if ("senha-ficticia".equals(existente.getPassword())) {
            existente.setPassword(passwordHasher.encode("Admin@123"));
            administradorRepository.save(existente);
        }
    }

    private void criarMaterialSeAusente(String nome) {
        materialRepository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> materialRepository.save(new Material(null, nome)));
    }
}