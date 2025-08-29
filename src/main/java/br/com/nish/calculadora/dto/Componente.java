package br.com.nish.calculadora.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item do detalhamento do cálculo de rescisão.
 * Representa uma verba individual com seu valor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Componente {

    private String nome;

    private BigDecimal valor;
}
