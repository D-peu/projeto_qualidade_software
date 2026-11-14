package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reserva from Reserva reserva where reserva.id = :reservaId")
    Optional<Reserva> buscarPorIdComBloqueio(@Param("reservaId") Long reservaId);

    @EntityGraph(attributePaths = {"professores", "materiais"})
    Optional<Reserva> findById(Long id);

    @EntityGraph(attributePaths = {"professores", "materiais"})
    List<Reserva> findBySolicitanteIdOrderByInicioDesc(Long solicitanteId);

    @EntityGraph(attributePaths = {"professores", "materiais"})
    List<Reserva> findByStatusOrderBySolicitadoEmAsc(StatusReserva status);

    @Query("""
            select count(reserva) from Reserva reserva
            where reserva.sala.id = :salaId
              and reserva.status in :status
              and reserva.inicio < :fim
              and reserva.fim > :inicio
              and (:reservaId is null or reserva.id <> :reservaId)
            """)
    long contarConflitos(
            @Param("salaId") Long salaId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @Query("""
            select count(distinct reserva) from Reserva reserva
            join reserva.professores professor
            where professor.id = :professorId
              and reserva.status in :status
              and reserva.inicio < :fim
              and reserva.fim > :inicio
              and (:reservaId is null or reserva.id <> :reservaId)
            """)
    long contarConflitosPorProfessor(
            @Param("professorId") Long professorId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @Query("""
            select count(distinct reserva) from Reserva reserva
            join reserva.materiais material
            where material.id = :materialId
              and reserva.status in :status
              and reserva.inicio < :fim
              and reserva.fim > :inicio
              and (:reservaId is null or reserva.id <> :reservaId)
            """)
    long contarConflitosPorMaterial(
            @Param("materialId") Long materialId,
            @Param("status") Collection<StatusReserva> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("reservaId") Long reservaId
    );

    @Query("select reserva.sala.id from Reserva reserva where reserva.id = :reservaId")
    Optional<Long> findSalaIdByReservaId(@Param("reservaId") Long reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reserva from Reserva reserva where reserva.sala.id = :salaId order by reserva.id")
    List<Reserva> findAllBySalaIdForUpdate(@Param("salaId") Long salaId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Reserva reserva
            set reserva.inicio = :inicio, reserva.fim = :fim
            where reserva.id = :reservaId and reserva.status = :status
            """)
    int atualizarHorarioAprovado(
            @Param("reservaId") Long reservaId,
            @Param("status") StatusReserva status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reserva from Reserva reserva
            where reserva.status = :solicitada
              and reserva.inicio <= :instante
            order by reserva.inicio, reserva.id
            """)
    List<Reserva> buscarSolicitacoesVencidasComBloqueio(
            @Param("instante") LocalDateTime instante,
            @Param("solicitada") StatusReserva solicitada
    );
}
