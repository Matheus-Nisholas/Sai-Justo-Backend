package br.com.nish.calculadora.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de usuários.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
