package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("select reserva.sala.id from Reserva reserva where reserva.id = :reservaId")
    Optional<Long> findSalaIdByReservaId(@Param("reservaId") Long reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reserva from Reserva reserva where reserva.sala.id = :salaId order by reserva.id")
    List<Reserva> findAllBySalaIdForUpdate(@Param("salaId") Long salaId);
}
