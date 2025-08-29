package br.com.nish.calculadora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para cálculo de rescisão.
 * Usa validações básicas e Lombok para reduzir boilerplate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoRescisaoRequest {

    @NotNull
    private TipoRescisao tipoRescisao;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal salarioMensal;

    @NotNull
    private LocalDate dataAdmissao;

    @NotNull
    private LocalDate dataDesligamento;

    private boolean avisoIndenizado;

    /**
     * Dias de férias vencidas ainda não gozadas.
     */
    private int feriasVencidasDias;

    /**
     * Meses trabalhados no ano corrente para cálculo proporcional.
     */
    private int mesesTrabalhadosNoAnoAtual;

    /**
     * Total já depositado de FGTS para o contrato em questão.
     */
    @DecimalMin("0.0")
    private BigDecimal saldoFgtsDepositado;
}
