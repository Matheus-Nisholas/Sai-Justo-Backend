package br.com.nish.calculadora.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para histórico de cálculos.
 */
public interface CalculoRescisaoRepository extends JpaRepository<CalculoRescisao, Long> {
    Page<CalculoRescisao> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);
}
