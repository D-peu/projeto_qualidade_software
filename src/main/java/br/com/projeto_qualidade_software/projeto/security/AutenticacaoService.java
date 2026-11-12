package br.com.projeto_qualidade_software.projeto.security;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class AutenticacaoService {

    private final SolicitanteRepository solicitanteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordHasher passwordHasher;

    public AutenticacaoService(
            SolicitanteRepository solicitanteRepository,
            ResponsavelRepository responsavelRepository,
            AdministradorRepository administradorRepository,
            PasswordHasher passwordHasher
    ) {
        this.solicitanteRepository = solicitanteRepository;
        this.responsavelRepository = responsavelRepository;
        this.administradorRepository = administradorRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public UsuarioAutenticado autenticar(String email, String senha) {
        if (email == null || senha == null) {
            throw new CredenciaisInvalidasException();
        }
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        List<ContaEncontrada> contas = buscarContas(emailNormalizado);
        if (contas.size() != 1) {
            throw new CredenciaisInvalidasException();
        }

        ContaEncontrada conta = contas.get(0);
        if (!passwordHasher.matches(senha, conta.senha())) {
            throw new CredenciaisInvalidasException();
        }
        if (passwordHasher.precisaAtualizar(conta.senha())) {
            conta.atualizarSenha().accept(passwordHasher.encode(senha));
        }
        return conta.usuario();
    }

    private List<ContaEncontrada> buscarContas(String email) {
        List<ContaEncontrada> contas = new ArrayList<>();
        solicitanteRepository.findByEmailIgnoreCase(email).ifPresent(solicitante ->
                contas.add(contaSolicitante(solicitante)));
        responsavelRepository.findByEmailIgnoreCase(email).ifPresent(responsavel ->
                contas.add(contaResponsavel(responsavel)));
        administradorRepository.findByEmailIgnoreCase(email).ifPresent(administrador ->
                contas.add(contaAdministrador(administrador)));
        return contas;
    }

    private ContaEncontrada contaSolicitante(Solicitante solicitante) {
        return new ContaEncontrada(
                solicitante.getPassword(),
                new UsuarioAutenticado(
                        solicitante.getId(),
                        solicitante.getName(),
                        solicitante.getEmail(),
                        PerfilUsuario.SOLICITANTE,
                        solicitante.getTipoSolicitante()
                ),
                senha -> {
                    solicitante.setPassword(senha);
                    solicitanteRepository.save(solicitante);
                }
        );
    }

    private ContaEncontrada contaResponsavel(Responsavel responsavel) {
        return new ContaEncontrada(
                responsavel.getPassword(),
                new UsuarioAutenticado(
                        responsavel.getId(),
                        responsavel.getName(),
                        responsavel.getEmail(),
                        PerfilUsuario.RESPONSAVEL,
                        null
                ),
                senha -> {
                    responsavel.setPassword(senha);
                    responsavelRepository.save(responsavel);
                }
        );
    }

    private ContaEncontrada contaAdministrador(Administrador administrador) {
        return new ContaEncontrada(
                administrador.getPassword(),
                new UsuarioAutenticado(
                        administrador.getId(),
                        administrador.getName(),
                        administrador.getEmail(),
                        PerfilUsuario.ADMINISTRADOR,
                        null
                ),
                senha -> {
                    administrador.setPassword(senha);
                    administradorRepository.save(administrador);
                }
        );
    }

    private record ContaEncontrada(
            String senha,
            UsuarioAutenticado usuario,
            Consumer<String> atualizarSenha
    ) {
    }
}
