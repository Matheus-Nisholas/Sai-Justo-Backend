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

    @Min(0)
    private int numeroDependentes;

    // NOVO: Campo para receber o nome do empregado do frontend.
    private String nomeEmpregado;
}