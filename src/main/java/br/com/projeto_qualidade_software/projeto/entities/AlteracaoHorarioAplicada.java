package br.com.projeto_qualidade_software.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "alteracoes_horario_aplicadas")
public class AlteracaoHorarioAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false, updatable = false)
    private Responsavel responsavel;

    @Column(name = "old_inicio", nullable = false, updatable = false)
    private LocalDateTime oldInicio;

    @Column(name = "old_fim", nullable = false, updatable = false)
    private LocalDateTime oldFim;

    @Column(name = "new_inicio", nullable = false, updatable = false)
    private LocalDateTime newInicio;

    @Column(name = "new_fim", nullable = false, updatable = false)
    private LocalDateTime newFim;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    protected AlteracaoHorarioAplicada() {
    }

    public AlteracaoHorarioAplicada(
            Reserva reserva,
            Responsavel responsavel,
            LocalDateTime oldInicio,
            LocalDateTime oldFim,
            LocalDateTime newInicio,
            LocalDateTime newFim,
            Instant appliedAt
    ) {
        this.reserva = reserva;
        this.responsavel = responsavel;
        this.oldInicio = oldInicio;
        this.oldFim = oldFim;
        this.newInicio = newInicio;
        this.newFim = newFim;
        this.appliedAt = appliedAt;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public LocalDateTime getOldInicio() {
        return oldInicio;
    }

    public LocalDateTime getOldFim() {
        return oldFim;
    }

    public LocalDateTime getNewInicio() {
        return newInicio;
    }

    public LocalDateTime getNewFim() {
        return newFim;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}
