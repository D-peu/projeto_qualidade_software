package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.AlteracaoHorarioAplicada;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AlteracaoHorarioAplicadaRepository
        extends Repository<AlteracaoHorarioAplicada, Long> {

    AlteracaoHorarioAplicada save(AlteracaoHorarioAplicada alteracao);

    List<AlteracaoHorarioAplicada> findAllByReserva_IdOrderByAppliedAtAscIdAsc(Long reservaId);
}
