package br.com.nish.calculadora.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por calcular os descontos legais (INSS, IRRF).
 */
@Service
public class DescontosService {

    // Tabela INSS 2025 (Valores e alíquotas fictícios baseados em projeções comuns)
    private static final List<FaixaInss> TABELA_INSS_2025 = List.of(
            new FaixaInss(new BigDecimal("1500.00"), new BigDecimal("0.075")),
            new FaixaInss(new BigDecimal("2800.00"), new BigDecimal("0.09")),
            new FaixaInss(new BigDecimal("4200.00"), new BigDecimal("0.12")),
            new FaixaInss(new BigDecimal("7800.00"), new BigDecimal("0.14"))
    );

    // ALTERADO: Adicionada a tabela do IRRF (baseada na tabela vigente, para o ano de 2025)
    private static final BigDecimal DEDUCAO_POR_DEPENDENTE_2025 = new BigDecimal("189.59");
    private static final List<FaixaIrrf> TABELA_IRRF_2025 = List.of(
            new FaixaIrrf(new BigDecimal("2259.20"), BigDecimal.ZERO, BigDecimal.ZERO),
            new FaixaIrrf(new BigDecimal("2826.65"), new BigDecimal("0.075"), new BigDecimal("169.44")),
            new FaixaIrrf(new BigDecimal("3751.05"), new BigDecimal("0.15"), new BigDecimal("381.44")),
            new FaixaIrrf(new BigDecimal("4664.68"), new BigDecimal("0.225"), new BigDecimal("662.77")),
            new FaixaIrrf(null, new BigDecimal("0.275"), new BigDecimal("896.00")) // Faixa final (acima de 4.664,68)
    );

    public BigDecimal calcularInss(BigDecimal baseCalculo) {
        if (baseCalculo == null || baseCalculo.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal descontoTotal = BigDecimal.ZERO;
        BigDecimal baseFaixaAnterior = BigDecimal.ZERO;

        for (FaixaInss faixa : TABELA_INSS_2025) {
            BigDecimal tetoFaixa = faixa.teto();
            BigDecimal baseNestaFaixa = baseCalculo.min(tetoFaixa).subtract(baseFaixaAnterior);

            if (baseNestaFaixa.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal valorDescontoFaixa = baseNestaFaixa.multiply(faixa.aliquota());
                descontoTotal = descontoTotal.add(valorDescontoFaixa);
            }

            baseFaixaAnterior = tetoFaixa;

            if (baseCalculo.compareTo(tetoFaixa) <= 0) {
                break;
            }
        }

        return descontoTotal.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ALTERADO: Novo método para calcular o IRRF.
     * Calcula o valor do desconto de IRRF com base na base tributável,
     * subtraindo as deduções legais (INSS, dependentes).
     *
     * @param baseTributavel   Valor bruto que serve de base (ex: Saldo de Salário).
     * @param inssDescontado   Valor já calculado de INSS sobre essa base.
     * @param numeroDependentes Quantidade de dependentes para dedução.
     * @return O valor do desconto de IRRF.
     */
    public BigDecimal calcularIrrf(BigDecimal baseTributavel, BigDecimal inssDescontado, int numeroDependentes) {
        if (baseTributavel == null || baseTributavel.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal deducaoDependentes = DEDUCAO_POR_DEPENDENTE_2025.multiply(new BigDecimal(numeroDependentes));
        BigDecimal baseDeCalculoFinal = baseTributavel.subtract(inssDescontado).subtract(deducaoDependentes);

        if (baseDeCalculoFinal.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        FaixaIrrf faixaAplicavel = TABELA_IRRF_2025.stream()
                .filter(faixa -> faixa.teto() == null || baseDeCalculoFinal.compareTo(faixa.teto()) <= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma faixa de IRRF aplicável encontrada."));

        BigDecimal impostoBruto = baseDeCalculoFinal.multiply(faixaAplicavel.aliquota());
        BigDecimal impostoDevido = impostoBruto.subtract(faixaAplicavel.parcelaADeduzir());

        return impostoDevido.compareTo(BigDecimal.ZERO) > 0
                ? impostoDevido.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record FaixaInss(BigDecimal teto, BigDecimal aliquota) {}
    private record FaixaIrrf(BigDecimal teto, BigDecimal aliquota, BigDecimal parcelaADeduzir) {}
}