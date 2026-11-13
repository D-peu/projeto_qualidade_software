package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.api.dto.CriarSalaRequest;
import br.com.projeto_qualidade_software.projeto.api.dto.AlterarStatusSalaRequest;
import br.com.projeto_qualidade_software.projeto.api.dto.SalaResponse;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.services.SalaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public List<SalaResponse> listar(
            @RequestParam(required = false) StatusSala status,
            @RequestParam(required = false) Integer capacidadeMinima
    ) {
        return salaService.listarSalas(status, capacidadeMinima)
                .stream()
                .map(SalaResponse::de)
                .toList();
    }

    @PostMapping
    public ResponseEntity<SalaResponse> criar(@Valid @RequestBody CriarSalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SalaResponse.de(salaService.criarSala(request.capacidade())));
    }

    @PutMapping("/{salaId}/status")
    public SalaResponse alterarStatus(
            @PathVariable Long salaId,
            @Valid @RequestBody AlterarStatusSalaRequest request
    ) {
        return SalaResponse.de(salaService.alterarStatus(salaId, request.statusSala()));
    }
}
