package br.com.projeto_qualidade_software.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "administradores")
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private LocalDate criadoEm;

    private LocalDate atualizadoEm;

    public Administrador() {
    }

    public Administrador(
            Long id,
            String name,
            String email,
            String password,
            LocalDate criadoEm,
            LocalDate atualizadoEm
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    protected void aoCriar() {
        criadoEm = LocalDate.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    protected void aoAtualizar() {
        atualizadoEm = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getCriadoEm() {
        return criadoEm;
    }

    public LocalDate getAtualizadoEm() {
        return atualizadoEm;
    }
}