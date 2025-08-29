package br.com.nish.calculadora.auth;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Perfil de acesso (ex.: ROLE_USER).
 */
@Entity
@Table(name = "roles")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
