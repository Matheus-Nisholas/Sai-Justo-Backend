package br.com.nish.calculadora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para cálculo de rescisão.
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

    private int feriasVencidasDias;

    private int mesesTrabalhadosNoAnoAtual;

    @DecimalMin("0.0")
    private BigDecimal saldoFgtsDepositado;

    // ALTERADO: Campo adicionado para o cálculo de dedução do IRRF.
    @Min(0)
    private int numeroDependentes;
}