package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {

    Optional<Solicitante> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Solicitante s
            where s.id = :id and s.tipoSolicitante = :tipo
            """)
    Optional<Solicitante> buscarProfessorPorIdComBloqueio(
            @Param("id") Long id,
            @Param("tipo") TipoSolicitante tipo
    );

    List<Solicitante> findByTipoSolicitanteOrderByNameAsc(TipoSolicitante tipoSolicitante);
}