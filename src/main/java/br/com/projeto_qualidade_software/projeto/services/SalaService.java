package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.security.ContextoAutenticacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salaRepository;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @Transactional(readOnly = true)
    public List<Sala> listarSalas() {
        return salaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Sala> listarSalas(StatusSala status, Integer capacidadeMinima) {
        if (status == null && capacidadeMinima == null) {
            return listarSalas();
        }
        return salaRepository.buscarPorFiltros(status, capacidadeMinima);
    }

    @Transactional
    public Sala criarSala(Integer capacidade) {
        ContextoAutenticacao.exigirAdministrador();
        return salaRepository.save(new Sala(null, capacidade, StatusSala.DISPONIVEL));
    }

    @Transactional
    public Sala alterarStatus(Long salaId, StatusSala status) {
        ContextoAutenticacao.exigirAdministrador();
        Sala sala = salaRepository.buscarPorIdComBloqueio(salaId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Sala não encontrada: " + salaId
                ));
        sala.setStatusSala(status);
        return salaRepository.save(sala);
    }
}
