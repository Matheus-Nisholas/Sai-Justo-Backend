package br.com.nish.calculadora.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de roles.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
