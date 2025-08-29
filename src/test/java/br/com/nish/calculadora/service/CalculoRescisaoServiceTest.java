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
}
