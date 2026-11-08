package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> buscarPorIdComBloqueio(@Param("id") Long id);

    @EntityGraph(attributePaths = {"professores", "materiais"})
    Optional<Reserva> findById(Long id);

    @Query("""
            select count(r) from Reserva r
            where r.sala.id = :salaId
              and r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
              and (:reservaId is null or r.id <> :reservaId)
            """)
    long contarConflitos(
            @Param("salaId") Long salaId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @Query("""
            select count(distinct r) from Reserva r
            join r.professores p
            where p.id = :professorId
              and r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
              and (:reservaId is null or r.id <> :reservaId)
            """)
    long contarConflitosPorProfessor(
            @Param("professorId") Long professorId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @Query("""
            select count(distinct r) from Reserva r
            join r.materiais m
            where m.id = :materialId
              and r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
              and (:reservaId is null or r.id <> :reservaId)
            """)
    long contarConflitosPorMaterial(
            @Param("materialId") Long materialId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @EntityGraph(attributePaths = {"professores", "materiais"})
    List<Reserva> findBySolicitanteIdOrderByInicioDesc(Long solicitanteId);

    @EntityGraph(attributePaths = {"professores", "materiais"})
    List<Reserva> findByStatusOrderBySolicitadoEmAsc(StatusReserva status);
}