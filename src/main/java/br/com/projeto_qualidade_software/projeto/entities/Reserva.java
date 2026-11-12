package br.com.projeto_qualidade_software.projeto.entities;

import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Solicitante solicitante;

    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private Responsavel responsavel;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime inicio;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.SOLICITADA;

    @Column(nullable = false, updatable = false)
    private LocalDateTime solicitadoEm;

    private LocalDateTime aprovadoEm;

    public Reserva() {
    }

    public Reserva(
            Long id,
            Sala sala,
            Solicitante solicitante,
            Responsavel responsavel,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva status,
            LocalDateTime solicitadoEm,
            LocalDateTime aprovadoEm
    ) {
        this.id = id;
        this.sala = sala;
        this.solicitante = solicitante;
        this.responsavel = responsavel;
        this.inicio = inicio;
        this.fim = fim;
        this.status = status;
        this.solicitadoEm = solicitadoEm;
        this.aprovadoEm = aprovadoEm;
    }

    @PrePersist
    protected void aoCriar() {
        if (solicitadoEm == null) {
            solicitadoEm = LocalDateTime.now();
        }

        if (status == null) {
            status = StatusReserva.SOLICITADA;
        }
    }

    @AssertTrue(message = "O término deve ser posterior ao início.")
    public boolean isIntervaloValido() {
        return inicio == null || fim == null || fim.isAfter(inicio);
    }

    public void aprovar(Responsavel responsavel) {
        if (responsavel == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o responsável pela aprovação."
            );
        }

        if (status != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas podem ser aprovadas."
            );
        }

        this.responsavel = responsavel;
        this.status = StatusReserva.APROVADA;
        this.aprovadoEm = LocalDateTime.now();
    }

    public void rejeitar(Responsavel responsavel) {
        if (responsavel == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o responsável pela decisão."
            );
        }

        if (status != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas podem ser rejeitadas."
            );
        }

        this.responsavel = responsavel;
        this.status = StatusReserva.REJEITADA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Responsavel responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public LocalDateTime getSolicitadoEm() {
        return solicitadoEm;
    }

    public void setSolicitadoEm(LocalDateTime solicitadoEm) {
        this.solicitadoEm = solicitadoEm;
    }

    public LocalDateTime getAprovadoEm() {
        return aprovadoEm;
    }

    public void setAprovadoEm(LocalDateTime aprovadoEm) {
        this.aprovadoEm = aprovadoEm;
    }
}