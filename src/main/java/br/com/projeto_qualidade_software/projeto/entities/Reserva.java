package br.com.projeto_qualidade_software.projeto.entities;

import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @ManyToOne
    @JoinColumn(name = "administrador_id")
    private Administrador administrador;

    @ManyToMany
    @JoinTable(
            name = "reservas_professores",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "professor_id")
    )
    private Set<Solicitante> professores = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reservas_materiais",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    private Set<Material> materiais = new LinkedHashSet<>();

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

        validarSolicitadaParaDecisao();
        this.responsavel = responsavel;
        this.administrador = null;
        registrarAprovacao();
    }

    public void rejeitar(Responsavel responsavel) {
        if (responsavel == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o responsável pela decisão."
            );
        }

        validarSolicitadaParaDecisao();
        this.responsavel = responsavel;
        this.administrador = null;
        this.status = StatusReserva.REJEITADA;
    }

    public void rejeitar(Administrador administrador) {
        if (administrador == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o administrador pela decisão."
            );
        }

        validarSolicitadaParaDecisao();
        this.responsavel = null;
        this.administrador = administrador;
        this.status = StatusReserva.REJEITADA;
    }

    public void validarSolicitadaParaDecisao() {
        if (status != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas podem receber uma decisão."
            );
        }
    }

    public void iniciarUtilizacao() {
        validarPodeIniciarUtilizacao();
        status = StatusReserva.EM_USO;
    }

    public void concluirUtilizacao() {
        validarPodeConcluirUtilizacao();
        status = StatusReserva.CONCLUIDA;
    }

    public void registrarNaoComparecimento() {
        validarPodeRegistrarNaoComparecimento();
        status = StatusReserva.NAO_COMPARECEU;
    }

    public void validarPodeIniciarUtilizacao() {
        if (status != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas aprovadas podem iniciar a utilização."
            );
        }
    }

    public void validarPodeConcluirUtilizacao() {
        if (status != StatusReserva.EM_USO) {
            throw new IllegalStateException(
                    "Somente reservas em uso podem ser concluídas."
            );
        }
    }

    public void validarPodeRegistrarNaoComparecimento() {
        if (status != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas aprovadas podem registrar não comparecimento."
            );
        }
    }

    private void registrarAprovacao() {
        this.status = StatusReserva.APROVADA;
        this.aprovadoEm = LocalDateTime.now();
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

    public Administrador getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Administrador administrador) {
        this.administrador = administrador;
    }

    public Set<Solicitante> getProfessores() {
        return professores;
    }

    public void setProfessores(Set<Solicitante> professores) {
        this.professores = professores == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(professores);
    }

    public Set<Material> getMateriais() {
        return materiais;
    }

    public void setMateriais(Set<Material> materiais) {
        this.materiais = materiais == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(materiais);
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
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