package br.com.nish.calculadora.service;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.dto.TipoRescisao;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculoRescisaoServiceTest {

    private final CalculoRescisaoService service = new CalculoRescisaoService();

    @Test
    void deveCalcularSaldoDeSalario_porDiasDoMes() {
        // Salário 3000 -> diário 100; desligamento dia 15 -> 100 * 15 = 1500
        BigDecimal res = service.calcularSaldoSalario(new BigDecimal("3000.00"), LocalDate.of(2025, 8, 15));
        assertEquals(new BigDecimal("1500.00"), res);
    }

    @Test
    void deveCalcularDecimoTerceiroProporcional_porMeses() {
        // 3000 * (8/12) = 2000
        BigDecimal res = service.calcularDecimoTerceiroProporcional(new BigDecimal("3000.00"), 8);
        assertEquals(new BigDecimal("2000.00"), res);
    }

    @Test
    void fluxoCompletoSaldoMais13_produzTotaisEsperados() {
        CalculoRescisaoRequest req = CalculoRescisaoRequest.builder()
                .tipoRescisao(TipoRescisao.SEM_JUSTA_CAUSA)
                .salarioMensal(new BigDecimal("3000.00"))
                .dataAdmissao(LocalDate.of(2023, 1, 10))
                .dataDesligamento(LocalDate.of(2025, 8, 15)) // saldo = 1500
                .avisoIndenizado(true)
                .feriasVencidasDias(0)
                .mesesTrabalhadosNoAnoAtual(8) // 13º = 2000
                .saldoFgtsDepositado(new BigDecimal("5000.00"))
                .build();

        CalculoRescisaoResponse resp = service.calcular(req);

        assertEquals(new BigDecimal("3500.00"), resp.getTotalBruto());     // 1500 + 2000
        assertEquals(new BigDecimal("0.00"), resp.getTotalDescontos());
        assertEquals(new BigDecimal("3500.00"), resp.getTotalLiquido());
    }
    @Test
    void deveCalcularFeriasProporcionaisMaisUmTerco() {
        // salário 3000; meses=8 -> base 3000*(8/12)=2000; +1/3 => 2000 + 666.67 = 2666.67
        CalculoRescisaoService svc = new CalculoRescisaoService();
        BigDecimal res = svc.calcularFeriasProporcionaisMaisUmTerco(new BigDecimal("3000.00"), 8);
        assertEquals(new BigDecimal("2666.67"), res);
    }

    @Test
    void deveCalcularFeriasVencidasMaisUmTerco_porDias() {
        // salário 3000; diário=100; 10 dias => 1000; +1/3 => 1333.33
        CalculoRescisaoService svc = new CalculoRescisaoService();
        BigDecimal res = svc.calcularFeriasVencidasMaisUmTerco(new BigDecimal("3000.00"), 10);
        assertEquals(new BigDecimal("1333.33"), res);
    }

    @Test
    void fluxoIncluiFeriasProporcionais_e_FeriasVencidasSeHouver() {
        CalculoRescisaoService svc = new CalculoRescisaoService();

        CalculoRescisaoResponse resp = svc.calcular(
                CalculoRescisaoRequest.builder()
                        .tipoRescisao(br.com.nish.calculadora.dto.TipoRescisao.SEM_JUSTA_CAUSA)
                        .salarioMensal(new BigDecimal("3000.00"))
                        .dataAdmissao(LocalDate.of(2023, 1, 10))
                        .dataDesligamento(LocalDate.of(2025, 8, 15)) // saldo = 1500
                        .avisoIndenizado(true)
                        .feriasVencidasDias(10) // 1333.33
                        .mesesTrabalhadosNoAnoAtual(8) // 13º = 2000; férias prop = 2666.67
                        .saldoFgtsDepositado(new BigDecimal("5000.00"))
                        .build()
        );

        // Totais esperados: 1500 + 2000 + 2666.67 + 1333.33 = 7500.00
        assertEquals(new BigDecimal("7500.00"), resp.getTotalBruto());
        assertEquals(new BigDecimal("0.00"), resp.getTotalDescontos());
        assertEquals(new BigDecimal("7500.00"), resp.getTotalLiquido());
    }
}
