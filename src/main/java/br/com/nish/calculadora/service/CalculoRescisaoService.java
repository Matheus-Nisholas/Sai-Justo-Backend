package br.com.nish.calculadora.service;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.dto.Componente;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por executar o cálculo das verbas rescisórias.
 * Incremento 1: saldo de salário + 13º proporcional (reais), demais verbas virão nas próximas etapas.
 */
@Service
public class CalculoRescisaoService {

    private static final BigDecimal TRINTA = new BigDecimal("30");
    private static final BigDecimal DOZE   = new BigDecimal("12");

    public CalculoRescisaoResponse calcular(CalculoRescisaoRequest req) {
        List<Componente> componentes = new ArrayList<Componente>();

        BigDecimal saldoSalario = calcularSaldoSalario(req.getSalarioMensal(), req.getDataDesligamento());
        componentes.add(new Componente("Saldo de salário", saldoSalario));

        BigDecimal decimoProporcional = calcularDecimoTerceiroProporcional(
                req.getSalarioMensal(), req.getMesesTrabalhadosNoAnoAtual()
        );
        componentes.add(new Componente("13º proporcional", decimoProporcional));

        // NOVO: férias proporcionais + 1/3 e férias vencidas (opcional)
        BigDecimal feriasPropMaisTerco = calcularFeriasProporcionaisMaisUmTerco(
                req.getSalarioMensal(), req.getMesesTrabalhadosNoAnoAtual()
        );
        componentes.add(new Componente("Férias proporcionais + 1/3", feriasPropMaisTerco));

        if (req.getFeriasVencidasDias() > 0) {
            BigDecimal feriasVencidas = calcularFeriasVencidasMaisUmTerco(
                    req.getSalarioMensal(), req.getFeriasVencidasDias()
            );
            componentes.add(new Componente("Férias vencidas + 1/3", feriasVencidas));
        }

        BigDecimal totalBruto = soma(componentes);
        BigDecimal totalDescontos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLiquido = totalBruto.subtract(totalDescontos).setScale(2, RoundingMode.HALF_UP);

        return CalculoRescisaoResponse.builder()
                .componentes(componentes)
                .totalBruto(totalBruto)
                .totalDescontos(totalDescontos)
                .totalLiquido(totalLiquido)
                .pagamentoAte(req.getDataDesligamento().plusDays(10))
                .build();
    }

    /** Férias proporcionais = (salário * meses/12) + 1/3 sobre esse valor. */
    BigDecimal calcularFeriasProporcionaisMaisUmTerco(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal baseProp = salarioMensal.multiply(new BigDecimal(mesesNoAno))
                .divide(DOZE, 10, RoundingMode.HALF_UP);
        BigDecimal umTerco = baseProp.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return baseProp.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    /** Férias vencidas em dias corridos (ex.: 10 dias) + 1/3. */
    BigDecimal calcularFeriasVencidasMaisUmTerco(BigDecimal salarioMensal, int diasVencidos) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        BigDecimal base = diario.multiply(new BigDecimal(diasVencidos));
        BigDecimal umTerco = base.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return base.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    /** Saldo de salário = (salário / 30) * dias trabalhados no mês do desligamento. */
    BigDecimal calcularSaldoSalario(BigDecimal salarioMensal, LocalDate dataDesligamento) {
        int diasTrabalhadosNoMes = dataDesligamento.getDayOfMonth(); // considera que trabalhou até o dia do desligamento
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasTrabalhadosNoMes)).setScale(2, RoundingMode.HALF_UP);
    }

    /** 13º proporcional = salário * (meses trabalhados no ano corrente) / 12. */
    BigDecimal calcularDecimoTerceiroProporcional(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal proporcao = new BigDecimal(mesesNoAno).divide(DOZE, 10, RoundingMode.HALF_UP);
        return salarioMensal.multiply(proporcao).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal soma(List<Componente> comps) {
        BigDecimal total = BigDecimal.ZERO;
        for (Componente c : comps) {
            total = total.add(c.getValor());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
