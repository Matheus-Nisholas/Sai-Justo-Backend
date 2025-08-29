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

        // >>> NOVO: Aviso prévio indenizado (só adiciona se for indenizado)
        if (req.isAvisoIndenizado()) {
            int diasAviso = calcularDiasAvisoPrevio(req.getDataAdmissao(), req.getDataDesligamento());
            BigDecimal avisoIndenizado = calcularAvisoPrevioIndenizado(req.getSalarioMensal(), diasAviso);
            componentes.add(new Componente("Aviso prévio indenizado (" + diasAviso + " dias)", avisoIndenizado));
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

    /** Saldo de salário = (salário / 30) * dias trabalhados no mês do desligamento. */
    BigDecimal calcularSaldoSalario(BigDecimal salarioMensal, LocalDate dataDesligamento) {
        int diasTrabalhadosNoMes = dataDesligamento.getDayOfMonth();
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasTrabalhadosNoMes)).setScale(2, RoundingMode.HALF_UP);
    }

    /** 13º proporcional = salário * (meses trabalhados no ano corrente) / 12. */
    BigDecimal calcularDecimoTerceiroProporcional(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal proporcao = new BigDecimal(mesesNoAno).divide(DOZE, 10, RoundingMode.HALF_UP);
        return salarioMensal.multiply(proporcao).setScale(2, RoundingMode.HALF_UP);
    }

    /** Férias proporcionais = (salário * meses/12) + 1/3 sobre esse valor. */
    BigDecimal calcularFeriasProporcionaisMaisUmTerco(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal baseProp = salarioMensal.multiply(new BigDecimal(mesesNoAno))
                .divide(DOZE, 10, RoundingMode.HALF_UP);
        BigDecimal umTerco = baseProp.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return baseProp.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    /** Férias vencidas em dias corridos + 1/3. */
    BigDecimal calcularFeriasVencidasMaisUmTerco(BigDecimal salarioMensal, int diasVencidos) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        BigDecimal base = diario.multiply(new BigDecimal(diasVencidos));
        BigDecimal umTerco = base.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return base.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Dias de aviso prévio: 30 + 3 dias por ano completo após o 1º, limitado a 90.
     */
    int calcularDiasAvisoPrevio(LocalDate dataAdmissao, LocalDate dataDesligamento) {
        if (dataAdmissao == null || dataDesligamento == null || dataDesligamento.isBefore(dataAdmissao)) {
            return 30; // fallback seguro
        }
        // anos completos de vínculo
        int anos = dataAdmissao.until(dataDesligamento).getYears();
        if (anos <= 1) {
            return 30;
        }
        int extra = (anos - 1) * 3;
        int total = 30 + extra;
        return Math.min(total, 90);
    }

    /**
     * Aviso prévio indenizado = (salário/30) * diasAviso.
     */
    BigDecimal calcularAvisoPrevioIndenizado(BigDecimal salarioMensal, int diasAviso) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasAviso)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal soma(List<Componente> comps) {
        BigDecimal total = BigDecimal.ZERO;
        for (Componente c : comps) {
            total = total.add(c.getValor());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}