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
    @Test
    void deveCalcularDiasAvisoPrevio_basico30Dias_seMenosDeUmAno() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        int dias = svc.calcularDiasAvisoPrevio(
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 8, 15)
        );
        assertEquals(30, dias);
    }

    @Test
    void deveCalcularDiasAvisoPrevio_comAcrescimoPorAnos_comLimite90() {
        CalculoRescisaoService svc = new CalculoRescisaoService();

        // 5 anos completos: 30 + (5-1)*3 = 42
        int dias5 = svc.calcularDiasAvisoPrevio(
                LocalDate.of(2020, 8, 1),
                LocalDate.of(2025, 8, 15)
        );
        assertEquals(42, dias5);

        // 25 anos completos -> 30 + 24*3 = 102 -> cap 90
        int diasCap = svc.calcularDiasAvisoPrevio(
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2025, 1, 2)
        );
        assertEquals(90, diasCap);
    }

    @Test
    void deveCalcularAvisoPrevioIndenizado_valorProporcionalAoSalario() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        // Salário 3000 -> diário 100; aviso 42 dias -> 4200.00
        assertEquals(
                new BigDecimal("4200.00"),
                svc.calcularAvisoPrevioIndenizado(new BigDecimal("3000.00"), 42)
        );
    }

    @Test
    void fluxoIncluiAvisoIndenizadoQuandoMarcado() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        // Com dados: saldo(15 dias) = 1500; 13º (8/12)=2000; férias prop=2666.67; aviso(42 dias)=4200
        CalculoRescisaoResponse resp = svc.calcular(
                CalculoRescisaoRequest.builder()
                        .tipoRescisao(br.com.nish.calculadora.dto.TipoRescisao.SEM_JUSTA_CAUSA)
                        .salarioMensal(new BigDecimal("3000.00"))
                        .dataAdmissao(LocalDate.of(2020, 8, 1))
                        .dataDesligamento(LocalDate.of(2025, 8, 15))
                        .avisoIndenizado(true)                 // inclui aviso
                        .feriasVencidasDias(0)
                        .mesesTrabalhadosNoAnoAtual(8)
                        .saldoFgtsDepositado(new BigDecimal("0.00"))
                        .build()
        );

        // 1500 + 2000 + 2666.67 + 4200 = 10366.67
        assertEquals(new BigDecimal("10366.67"), resp.getTotalBruto());
        assertEquals(new BigDecimal("0.00"), resp.getTotalDescontos());
        assertEquals(new BigDecimal("10366.67"), resp.getTotalLiquido());
    }

    @Test
    void fluxoNaoIncluiAvisoQuandoTrabalhado() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        // Mesmo cenário, mas avisoIndenizado=false -> sem a verba do aviso
        CalculoRescisaoResponse resp = svc.calcular(
                CalculoRescisaoRequest.builder()
                        .tipoRescisao(br.com.nish.calculadora.dto.TipoRescisao.SEM_JUSTA_CAUSA)
                        .salarioMensal(new BigDecimal("3000.00"))
                        .dataAdmissao(LocalDate.of(2020, 8, 1))
                        .dataDesligamento(LocalDate.of(2025, 8, 15))
                        .avisoIndenizado(false)
                        .feriasVencidasDias(0)
                        .mesesTrabalhadosNoAnoAtual(8)
                        .saldoFgtsDepositado(new BigDecimal("0.00"))
                        .build()
        );

        // 1500 + 2000 + 2666.67 = 6166.67
        assertEquals(new BigDecimal("6166.67"), resp.getTotalBruto());
    }

    @Test
    void deveCalcularMultaFgts40porCento_SemJustaCausa() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        BigDecimal multa = svc.calcularMultaFgts(
                br.com.nish.calculadora.dto.TipoRescisao.SEM_JUSTA_CAUSA,
                new BigDecimal("5000.00")
        );
        assertEquals(new BigDecimal("2000.00"), multa);
    }

    @Test
    void deveCalcularMultaFgts20porCento_Acordo484A() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        BigDecimal multa = svc.calcularMultaFgts(
                br.com.nish.calculadora.dto.TipoRescisao.ACORDO_484A,
                new BigDecimal("5000.00")
        );
        assertEquals(new BigDecimal("1000.00"), multa);
    }

    @Test
    void deveCalcularMultaFgtsZero_JustaCausaOuPedidoDemissao() {
        CalculoRescisaoService svc = new CalculoRescisaoService();
        BigDecimal multa = svc.calcularMultaFgts(
                br.com.nish.calculadora.dto.TipoRescisao.JUSTA_CAUSA,
                new BigDecimal("5000.00")
        );
        assertEquals(new BigDecimal("0.00"), multa);
    }

    @Test
    void fluxoIncluiSaldoFgtsEMulta() {
        CalculoRescisaoService svc = new CalculoRescisaoService();

        CalculoRescisaoResponse resp = svc.calcular(
                CalculoRescisaoRequest.builder()
                        .tipoRescisao(br.com.nish.calculadora.dto.TipoRescisao.SEM_JUSTA_CAUSA)
                        .salarioMensal(new BigDecimal("3000.00"))
                        .dataAdmissao(LocalDate.of(2020, 1, 1))
                        .dataDesligamento(LocalDate.of(2025, 8, 15))
                        .avisoIndenizado(false)
                        .feriasVencidasDias(0)
                        .mesesTrabalhadosNoAnoAtual(8)
                        .saldoFgtsDepositado(new BigDecimal("5000.00"))
                        .build()
        );

        // Deve incluir "Saldo FGTS depositado" (5000) e "Multa FGTS" (2000)
        boolean temSaldoFgts = resp.getComponentes().stream()
                .anyMatch(c -> c.getNome().contains("Saldo FGTS"));
        boolean temMultaFgts = resp.getComponentes().stream()
                .anyMatch(c -> c.getNome().contains("Multa FGTS"));

        assertEquals(true, temSaldoFgts);
        assertEquals(true, temMultaFgts);
    }

}
