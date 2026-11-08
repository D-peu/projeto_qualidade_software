package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogoRecursosController {

    private final SolicitanteRepository solicitanteRepository;
    private final MaterialRepository materialRepository;

    public CatalogoRecursosController(
            SolicitanteRepository solicitanteRepository,
            MaterialRepository materialRepository
    ) {
        this.solicitanteRepository = solicitanteRepository;
        this.materialRepository = materialRepository;
    }

    @GetMapping("/api/professores")
    public List<ProfessorResponse> listarProfessores() {
        return solicitanteRepository
                .findByTipoSolicitanteOrderByNameAsc(TipoSolicitante.PROFESSOR)
                .stream()
                .map(ProfessorResponse::de)
                .toList();
    }

    @GetMapping("/api/materiais")
    public List<MaterialResponse> listarMateriais() {
        return materialRepository.findAllByOrderByNomeAsc()
                .stream()
                .map(MaterialResponse::de)
                .toList();
    }

    public record ProfessorResponse(Long id, String nome) {
        static ProfessorResponse de(Solicitante professor) {
            return new ProfessorResponse(professor.getId(), professor.getName());
        }
    }

    public record MaterialResponse(Long id, String nome) {
        static MaterialResponse de(Material material) {
            return new MaterialResponse(material.getId(), material.getNome());
        }
    }
}
