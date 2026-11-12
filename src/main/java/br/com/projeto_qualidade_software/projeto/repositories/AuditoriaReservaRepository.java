package br.com.projeto_qualidade_software.projeto.repositories;

import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaReservaRepository extends JpaRepository<AuditoriaReserva, Long> {

    List<AuditoriaReserva> findByReservaIdOrderByDataHoraAsc(Long reservaId);
}
