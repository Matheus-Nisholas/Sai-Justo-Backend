package br.com.nish.calculadora.service;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.dto.Componente;
import br.com.nish.calculadora.dto.TipoRescisao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por executar o cálculo das verbas rescisórias.
 * Coberturas: saldo de salário, 13º proporcional, férias proporcionais + 1/3,
 * férias vencidas + 1/3, aviso prévio indenizado e FGTS + multa.
 * (A) Meses do ano são calculados automaticamente (regra ≥15 dias, com projeção de aviso).
 * (B) Policy por tipo de rescisão aplica inclusão/remoção de verbas e regras específicas.
 */
@Service
public class CalculoRescisaoService {

    private static final BigDecimal TRINTA = new BigDecimal("30");
    private static final BigDecimal DOZE   = new BigDecimal("12");
    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CalculoRescisaoResponse calcular(CalculoRescisaoRequest req) {
        List<Componente> componentes = new ArrayList<>();

        // (A) calcular meses no ano (ignora o campo mesesTrabalhadosNoAnoAtual do request)
        int mesesAno = calcularMesesTrabalhadosNoAno(req.getTipoRescisao(), req.getDataAdmissao(),
                req.getDataDesligamento(), req.isAvisoIndenizado());

        // Saldo de salário (sempre devido)
        BigDecimal saldoSalario = calcularSaldoSalario(req.getSalarioMensal(), req.getDataDesligamento());
        componentes.add(new Componente("Saldo de salário", saldoSalario));

        // (B) Policy por tipo
        boolean inclui13Prop = incluiDecimoProporcional(req.getTipoRescisao());
        boolean incluiFeriasProp = incluiFeriasProporcionais(req.getTipoRescisao());
        boolean incluiFeriasVencidas = req.getFeriasVencidasDias() > 0; // sempre que houver dias
        boolean podeTerAvisoIndenizado = incluiAvisoIndenizado(req.getTipoRescisao());

        if (inclui13Prop && mesesAno > 0) {
            BigDecimal decimoProporcional = calcularDecimoTerceiroProporcional(
                    req.getSalarioMensal(), mesesAno
            );
            componentes.add(new Componente("13º proporcional (" + mesesAno + " meses)", decimoProporcional));
        }

        if (incluiFeriasProp && mesesAno > 0) {
            BigDecimal feriasPropMaisTerco = calcularFeriasProporcionaisMaisUmTerco(
                    req.getSalarioMensal(), mesesAno
            );
            componentes.add(new Componente("Férias proporcionais + 1/3", feriasPropMaisTerco));
        }

        if (incluiFeriasVencidas) {
            BigDecimal feriasVencidas = calcularFeriasVencidasMaisUmTerco(
                    req.getSalarioMensal(), req.getFeriasVencidasDias()
            );
            componentes.add(new Componente("Férias vencidas + 1/3", feriasVencidas));
        }

        // Aviso prévio indenizado (policy específica para ACORDO 484-A = metade)
        if (req.isAvisoIndenizado() && podeTerAvisoIndenizado) {
            int diasAvisoCheio = calcularDiasAvisoPrevio(req.getDataAdmissao(), req.getDataDesligamento());
            int diasAvisoAplicados = diasAvisoCheio;
            if (req.getTipoRescisao() == TipoRescisao.ACORDO_484A) {
                diasAvisoAplicados = Math.max(0, diasAvisoCheio / 2); // metade
            }
            if (diasAvisoAplicados > 0) {
                BigDecimal avisoIndenizado = calcularAvisoPrevioIndenizado(req.getSalarioMensal(), diasAvisoAplicados);
                componentes.add(new Componente("Aviso prévio indenizado (" + diasAvisoAplicados + " dias)", avisoIndenizado));
            }
        }

        // FGTS + Multa
        BigDecimal saldoFgts = Objects.requireNonNullElse(req.getSaldoFgtsDepositado(), BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        componentes.add(new Componente("Saldo FGTS depositado", saldoFgts));

        BigDecimal multaFgts = calcularMultaFgts(req.getTipoRescisao(), saldoFgts);
        if (multaFgts.compareTo(BigDecimal.ZERO) > 0) {
            componentes.add(new Componente("Multa FGTS", multaFgts));
        }

        BigDecimal totalBruto = soma(componentes);
        BigDecimal totalDescontos = ZERO_2; // descontos viriam aqui numa próxima etapa
        BigDecimal totalLiquido = totalBruto.subtract(totalDescontos).setScale(2, RoundingMode.HALF_UP);

        return CalculoRescisaoResponse.builder()
                .componentes(componentes)
                .totalBruto(totalBruto)
                .totalDescontos(totalDescontos)
                .totalLiquido(totalLiquido)
                .pagamentoAte(req.getDataDesligamento().plusDays(10))
                .build();
    }

    // --------------------- Regras (A) meses no ano com projeção de aviso ---------------------

    /**
     * Calcula automaticamente os meses trabalhados no ano do desligamento
     * usando a regra: um mês conta se trabalhou ≥15 dias naquele mês.
     * Projeta o aviso prévio indenizado no vínculo (integra tempo de serviço).
     * - Para ACORDO_484A, projeta metade do aviso.
     */
    int calcularMesesTrabalhadosNoAno(TipoRescisao tipo, LocalDate adm, LocalDate deslig, boolean avisoIndenizado) {
        if (adm == null || deslig == null) return 0;

        int year = deslig.getYear();
        LocalDate inicioNoAno = LocalDate.of(year, 1, 1);
        LocalDate fimNoAno = LocalDate.of(year, 12, 31);

        // intervalo de vínculo considerado
        LocalDate inicioVinculo = adm.isAfter(inicioNoAno) ? adm : inicioNoAno;

        // projeção do aviso (se indenizado e policy permitir tempo)
        int diasAviso = avisoIndenizado ? calcularDiasAvisoPrevio(adm, deslig) : 0;
        if (tipo == TipoRescisao.ACORDO_484A && diasAviso > 0) {
            diasAviso = Math.max(0, diasAviso / 2); // metade no acordo
        }
        LocalDate fimEfetivo = avisoIndenizado ? deslig.plusDays(diasAviso) : deslig;

        // Recorta para o ano corrente
        LocalDate fimVinculo = fimEfetivo.isBefore(fimNoAno) ? fimEfetivo : fimNoAno;
        if (fimVinculo.isBefore(inicioNoAno) || fimVinculo.isBefore(inicioVinculo)) {
            return 0;
        }

        int meses = 0;
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate mesIni = ym.atDay(1);
            LocalDate mesFim = ym.atEndOfMonth();

            // sobreposição entre [mesIni, mesFim] e [inicioVinculo, fimVinculo]
            LocalDate ini = max(mesIni, inicioVinculo);
            LocalDate fim = min(mesFim, fimVinculo);

            if (!fim.isBefore(ini)) {
                long diasNoMes = java.time.temporal.ChronoUnit.DAYS.between(ini, fim) + 1; // inclusivo
                if (diasNoMes >= 15) {
                    meses++;
                }
            }
        }
        if (meses < 0) meses = 0;
        if (meses > 12) meses = 12;
        return meses;
    }

    private LocalDate max(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }

    // --------------------- Policy (B) por tipo de rescisão ---------------------

    boolean incluiDecimoProporcional(TipoRescisao tipo) {
        if (tipo == null) return true;
        switch (tipo) {
            case JUSTA_CAUSA:
                return false; // em regra, sem 13º proporcional
            default:
                return true;
        }
    }

    boolean incluiFeriasProporcionais(TipoRescisao tipo) {
        if (tipo == null) return true;
        switch (tipo) {
            case JUSTA_CAUSA:
                return false; // sem férias proporcionais
            default:
                return true;
        }
    }

    boolean incluiAvisoIndenizado(TipoRescisao tipo) {
        if (tipo == null) return true;
        switch (tipo) {
            case PEDIDO_DEMISSAO:
            case TERMO_CONTRATO:
            case JUSTA_CAUSA:
                return false; // não há aviso indenizado a favor do empregado
            default:
                return true;  // SEM_JUSTA_CAUSA e ACORDO_484A (metade do valor)
        }
    }

    /** Multa do FGTS: 40% (sem justa causa), 20% (acordo 484-A), 0% demais. */
    BigDecimal calcularMultaFgts(TipoRescisao tipo, BigDecimal saldoFgts) {
        if (tipo == null) return ZERO_2;
        switch (tipo) {
            case SEM_JUSTA_CAUSA:
                return saldoFgts.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
            case ACORDO_484A:
                return saldoFgts.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            default:
                return ZERO_2; // justa causa, pedido demissão, termo
        }
    }

    // --------------------- Fórmulas base já existentes ---------------------

    BigDecimal calcularSaldoSalario(BigDecimal salarioMensal, LocalDate dataDesligamento) {
        int diasTrabalhadosNoMes = dataDesligamento.getDayOfMonth();
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasTrabalhadosNoMes)).setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal calcularDecimoTerceiroProporcional(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal proporcao = new BigDecimal(mesesNoAno).divide(DOZE, 10, RoundingMode.HALF_UP);
        return salarioMensal.multiply(proporcao).setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal calcularFeriasProporcionaisMaisUmTerco(BigDecimal salarioMensal, int mesesNoAno) {
        BigDecimal baseProp = salarioMensal.multiply(new BigDecimal(mesesNoAno))
                .divide(DOZE, 10, RoundingMode.HALF_UP);
        BigDecimal umTerco = baseProp.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return baseProp.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal calcularFeriasVencidasMaisUmTerco(BigDecimal salarioMensal, int diasVencidos) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        BigDecimal base = diario.multiply(new BigDecimal(diasVencidos));
        BigDecimal umTerco = base.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
        return base.add(umTerco).setScale(2, RoundingMode.HALF_UP);
    }

    int calcularDiasAvisoPrevio(LocalDate dataAdmissao, LocalDate dataDesligamento) {
        if (dataAdmissao == null || dataDesligamento == null || dataDesligamento.isBefore(dataAdmissao)) {
            return 30; // fallback
        }
        int anos = dataAdmissao.until(dataDesligamento).getYears(); // anos completos
        if (anos <= 1) return 30;
        int extra = (anos - 1) * 3;
        int total = 30 + extra;
        return Math.min(total, 90);
    }

    BigDecimal calcularAvisoPrevioIndenizado(BigDecimal salarioMensal, int diasAviso) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasAviso)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal soma(List<Componente> comps) {
        BigDecimal total = BigDecimal.ZERO;
        for (Componente c : comps) total = total.add(c.getValor());
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
