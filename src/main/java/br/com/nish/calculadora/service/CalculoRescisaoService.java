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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculoRescisaoService {

    private final DescontosService descontosService;

    private static final BigDecimal TRINTA = new BigDecimal("30");
    private static final BigDecimal DOZE   = new BigDecimal("12");
    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CalculoRescisaoResponse calcular(CalculoRescisaoRequest req) {
        List<Componente> componentesProventos = new ArrayList<>();
        List<Componente> componentesDesconto = new ArrayList<>();

        int mesesAno = calcularMesesTrabalhadosNoAno(req.getTipoRescisao(), req.getDataAdmissao(),
                req.getDataDesligamento(), req.isAvisoIndenizado());

        // --- CÁLCULO DE PROVENTOS (VERBAS BRUTAS) ---
        BigDecimal saldoSalario = calcularSaldoSalario(req.getSalarioMensal(), req.getDataDesligamento());
        componentesProventos.add(new Componente("Saldo de salário", saldoSalario));

        boolean inclui13Prop = incluiDecimoProporcional(req.getTipoRescisao());
        BigDecimal decimoProporcional = BigDecimal.ZERO;
        if (inclui13Prop && mesesAno > 0) {
            decimoProporcional = calcularDecimoTerceiroProporcional(req.getSalarioMensal(), mesesAno);
            componentesProventos.add(new Componente("13º proporcional (" + mesesAno + " meses)", decimoProporcional));
        }

        if (incluiFeriasProporcionais(req.getTipoRescisao()) && mesesAno > 0) {
            BigDecimal feriasPropMaisTerco = calcularFeriasProporcionaisMaisUmTerco(req.getSalarioMensal(), mesesAno);
            componentesProventos.add(new Componente("Férias proporcionais + 1/3", feriasPropMaisTerco));
        }

        if (req.getFeriasVencidasDias() > 0) {
            BigDecimal feriasVencidas = calcularFeriasVencidasMaisUmTerco(req.getSalarioMensal(), req.getFeriasVencidasDias());
            componentesProventos.add(new Componente("Férias vencidas + 1/3", feriasVencidas));
        }

        if (req.isAvisoIndenizado() && incluiAvisoIndenizado(req.getTipoRescisao())) {
            int diasAvisoCheio = calcularDiasAvisoPrevio(req.getDataAdmissao(), req.getDataDesligamento());
            int diasAvisoAplicados = diasAvisoCheio;
            if (req.getTipoRescisao() == TipoRescisao.ACORDO_484A) {
                diasAvisoAplicados = Math.max(0, diasAvisoCheio / 2);
            }
            if (diasAvisoAplicados > 0) {
                BigDecimal avisoIndenizado = calcularAvisoPrevioIndenizado(req.getSalarioMensal(), diasAvisoAplicados);
                componentesProventos.add(new Componente("Aviso prévio indenizado (" + diasAvisoAplicados + " dias)", avisoIndenizado));
            }
        }

        BigDecimal saldoFgts = Objects.requireNonNullElse(req.getSaldoFgtsDepositado(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        componentesProventos.add(new Componente("Saldo FGTS depositado", saldoFgts));

        BigDecimal multaFgts = calcularMultaFgts(req.getTipoRescisao(), saldoFgts);
        if (multaFgts.compareTo(BigDecimal.ZERO) > 0) {
            componentesProventos.add(new Componente("Multa FGTS", multaFgts));
        }

        // --- CÁLCULO DE DESCONTOS ---
        // INSS
        BigDecimal inssSobreSalario = descontosService.calcularInss(saldoSalario);
        if (inssSobreSalario.compareTo(ZERO_2) > 0) {
            componentesDesconto.add(new Componente("INSS sobre Saldo de Salário", inssSobreSalario));
        }

        BigDecimal inssSobre13 = descontosService.calcularInss(decimoProporcional);
        if (inssSobre13.compareTo(ZERO_2) > 0) {
            componentesDesconto.add(new Componente("INSS sobre 13º Salário", inssSobre13));
        }

        // ALTERADO: Adicionado cálculo de IRRF
        BigDecimal irrfSobreSalario = descontosService.calcularIrrf(saldoSalario, inssSobreSalario, req.getNumeroDependentes());
        if (irrfSobreSalario.compareTo(ZERO_2) > 0) {
            componentesDesconto.add(new Componente("IRRF sobre Salário", irrfSobreSalario));
        }

        BigDecimal irrfSobre13 = descontosService.calcularIrrf(decimoProporcional, inssSobre13, req.getNumeroDependentes());
        if (irrfSobre13.compareTo(ZERO_2) > 0) {
            componentesDesconto.add(new Componente("IRRF sobre 13º Salário", irrfSobre13));
        }

        // --- TOTAIS ---
        BigDecimal totalBruto = somaComponentes(componentesProventos);
        BigDecimal totalDescontos = somaComponentes(componentesDesconto);
        BigDecimal totalLiquido = totalBruto.subtract(totalDescontos).setScale(2, RoundingMode.HALF_UP);

        return CalculoRescisaoResponse.builder()
                .componentes(componentesProventos)
                .descontos(componentesDesconto)
                .totalBruto(totalBruto)
                .totalDescontos(totalDescontos)
                .totalLiquido(totalLiquido)
                .pagamentoAte(req.getDataDesligamento().plusDays(10))
                .build();
    }

    // (O restante da classe, com todos os métodos auxiliares, permanece o mesmo)

    int calcularMesesTrabalhadosNoAno(TipoRescisao tipo, LocalDate adm, LocalDate deslig, boolean avisoIndenizado) {
        if (adm == null || deslig == null) return 0;
        int year = deslig.getYear();
        LocalDate inicioNoAno = LocalDate.of(year, 1, 1);
        LocalDate fimNoAno = LocalDate.of(year, 12, 31);
        LocalDate inicioVinculo = adm.isAfter(inicioNoAno) ? adm : inicioNoAno;

        int diasAviso = avisoIndenizado ? calcularDiasAvisoPrevio(adm, deslig) : 0;
        if (tipo == TipoRescisao.ACORDO_484A && diasAviso > 0) {
            diasAviso = Math.max(0, diasAviso / 2);
        }
        LocalDate fimEfetivo = avisoIndenizado ? deslig.plusDays(diasAviso) : deslig;
        LocalDate fimVinculo = fimEfetivo.isBefore(fimNoAno) ? fimEfetivo : fimNoAno;

        if (fimVinculo.isBefore(inicioNoAno) || fimVinculo.isBefore(inicioVinculo)) {
            return 0;
        }

        int meses = 0;
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate mesIni = ym.atDay(1);
            LocalDate mesFim = ym.atEndOfMonth();
            LocalDate ini = max(mesIni, inicioVinculo);
            LocalDate fim = min(mesFim, fimVinculo);

            if (!fim.isBefore(ini)) {
                long diasNoMes = java.time.temporal.ChronoUnit.DAYS.between(ini, fim) + 1;
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

    boolean incluiDecimoProporcional(TipoRescisao tipo) {
        if (tipo == null) return true;
        return tipo != TipoRescisao.JUSTA_CAUSA;
    }

    boolean incluiFeriasProporcionais(TipoRescisao tipo) {
        if (tipo == null) return true;
        return tipo != TipoRescisao.JUSTA_CAUSA;
    }

    boolean incluiAvisoIndenizado(TipoRescisao tipo) {
        if (tipo == null) return true;
        return switch (tipo) {
            case PEDIDO_DEMISSAO, TERMO_CONTRATO, JUSTA_CAUSA -> false;
            default -> true;
        };
    }

    BigDecimal calcularMultaFgts(TipoRescisao tipo, BigDecimal saldoFgts) {
        if (tipo == null) return ZERO_2;
        return switch (tipo) {
            case SEM_JUSTA_CAUSA -> saldoFgts.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
            case ACORDO_484A -> saldoFgts.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            default -> ZERO_2;
        };
    }

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
        BigDecimal baseProp = salarioMensal.multiply(new BigDecimal(mesesNoAno)).divide(DOZE, 10, RoundingMode.HALF_UP);
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
            return 30;
        }
        int anos = dataAdmissao.until(dataDesligamento).getYears();
        if (anos < 1) return 30;
        int extra = anos * 3;
        int total = 30 + extra;
        return Math.min(total, 90);
    }

    BigDecimal calcularAvisoPrevioIndenizado(BigDecimal salarioMensal, int diasAviso) {
        BigDecimal diario = salarioMensal.divide(TRINTA, 10, RoundingMode.HALF_UP);
        return diario.multiply(new BigDecimal(diasAviso)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal somaComponentes(List<Componente> comps) {
        BigDecimal total = BigDecimal.ZERO;
        for (Componente c : comps) total = total.add(c.getValor());
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}