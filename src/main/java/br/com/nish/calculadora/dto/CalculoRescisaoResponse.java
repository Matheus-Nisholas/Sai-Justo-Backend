package br.com.nish.calculadora.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de saída com o resultado e o breakdown das verbas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoRescisaoResponse {

    private BigDecimal totalBruto;

    private BigDecimal totalDescontos;

    private BigDecimal totalLiquido;

    private List<Componente> componentes;

    private LocalDate pagamentoAte;
}
