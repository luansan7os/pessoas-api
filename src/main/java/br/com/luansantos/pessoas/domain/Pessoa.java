package br.com.luansantos.pessoas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Pessoa registrada no sistema.
 *
 * O documento (CPF) e persistido sempre normalizado: apenas os 11 digitos,
 * sem pontuacao. A normalizacao acontece na borda (service), para que a
 * mesma pessoa nunca seja gravada duas vezes por causa da mascara.
 */
@Entity
@Table(name = "pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String documento;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 60)
    private String sobrenome;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Pessoa() {
        // exigido pelo JPA
    }

    public Pessoa(String documento, String nome, String sobrenome, String email) {
        this.documento = documento;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getNomeCompleto() {
        return nome + " " + sobrenome;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Pessoa pessoa)) {
            return false;
        }
        return id != null && Objects.equals(id, pessoa.id);
    }

    @Override
    public int hashCode() {
        return Pessoa.class.hashCode();
    }
}
