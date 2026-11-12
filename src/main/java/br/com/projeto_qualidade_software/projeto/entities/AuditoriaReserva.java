package br.com.projeto_qualidade_software.projeto.entities;

import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditorias_reserva")
public class AuditoriaReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private StatusReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "novo_estado", nullable = false)
    private StatusReserva novoEstado;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_usuario", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public AuditoriaReserva() {
    }

    public AuditoriaReserva(
            Long id,
            Reserva reserva,
            StatusReserva estadoAnterior,
            StatusReserva novoEstado,
            Long usuarioId,
            PerfilUsuario perfilUsuario,
            LocalDateTime dataHora
    ) {
        this.id = id;
        this.reserva = reserva;
        this.estadoAnterior = estadoAnterior;
        this.novoEstado = novoEstado;
        this.usuarioId = usuarioId;
        this.perfilUsuario = perfilUsuario;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public StatusReserva getEstadoAnterior() {
        return estadoAnterior;
    }

    public StatusReserva getNovoEstado() {
        return novoEstado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public PerfilUsuario getPerfilUsuario() {
        return perfilUsuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
