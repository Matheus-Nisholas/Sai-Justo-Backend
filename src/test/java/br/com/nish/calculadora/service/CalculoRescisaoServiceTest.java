package br.com.nish.calculadora.service;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.dto.TipoRescisao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@DisplayName("Testes para CalculoRescisaoService")
class CalculoRescisaoServiceTest {

    // ALTERADO: O serviço e sua dependência agora são instanciados no método de setup.
    private CalculoRescisaoService service;
    private DescontosService descontosServiceMock;

    @BeforeEach
    void setUp() {
        // ALTERADO: Criamos um mock do DescontosService.
        // Isso nos permite controlar o que seus métodos retornam durante os testes.
        descontosServiceMock = Mockito.mock(DescontosService.class);
        // ALTERADO: Injetamos o mock no construtor do serviço principal.
        service = new CalculoRescisaoService(descontosServiceMock);
    }

    @Test
    @DisplayName("Deve calcular saldo de salário corretamente por dias do mês")
    void deveCalcularSaldoDeSalario_porDiasDoMes() {
        // Salário 3000 -> diário 100; desligamento dia 15 -> 100 * 15 = 1500
        BigDecimal res = service.calcularSaldoSalario(new BigDecimal("3000.00"), LocalDate.of(2025, 8, 15));
        assertEquals(new BigDecimal("1500.00"), res);
    }

    @Test
    @DisplayName("Deve calcular 13º proporcional corretamente por meses")
    void deveCalcularDecimoTerceiroProporcional_porMeses() {
        // 3000 * (8/12) = 2000
        BigDecimal res = service.calcularDecimoTerceiroProporcional(new BigDecimal("3000.00"), 8);
        assertEquals(new BigDecimal("2000.00"), res);
    }

    @Test
    @DisplayName("Fluxo completo deve somar verbas brutas corretamente, ignorando descontos")
    void fluxoCompletoSaldoMais13_produzTotaisBrutosEsperados() {
        // ALTERADO: Para este teste focado no total bruto, configuramos o mock
        // para retornar ZERO para todos os cálculos de desconto.
        Mockito.when(descontosServiceMock.calcularInss(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
        Mockito.when(descontosServiceMock.calcularIrrf(any(BigDecimal.class), any(BigDecimal.class), anyInt())).thenReturn(BigDecimal.ZERO);

        CalculoRescisaoRequest req = CalculoRescisaoRequest.builder()
                .tipoRescisao(TipoRescisao.SEM_JUSTA_CAUSA)
                .salarioMensal(new BigDecimal("3000.00"))
                .dataAdmissao(LocalDate.of(2023, 1, 10))
                .dataDesligamento(LocalDate.of(2025, 8, 15)) // saldo = 1500
                .avisoIndenizado(false) // meses = 8
                .feriasVencidasDias(10) // Férias Vencidas + 1/3 = 1333.33
                .saldoFgtsDepositado(new BigDecimal("5000.00"))
                .numeroDependentes(0)
                .build();

        // Verbas esperadas:
        // Saldo Salário: 1500.00
        // 13º (8 meses): 2000.00
        // Férias Prop + 1/3 (8 meses): 2666.67
        // Férias Vencidas + 1/3 (10 dias): 1333.33
        // Saldo FGTS: 5000.00
        // Multa FGTS (40%): 2000.00
        // Total Bruto: 14500.00

        CalculoRescisaoResponse resp = service.calcular(req);

        assertEquals(new BigDecimal("14500.00"), resp.getTotalBruto());
        assertEquals(new BigDecimal("0.00"), resp.getTotalDescontos()); // Assegurado pelo mock
        assertEquals(new BigDecimal("14500.00"), resp.getTotalLiquido()); // Bruto - 0
    }

    @Test
    @DisplayName("Fluxo completo deve calcular totais incluindo descontos")
    void fluxoCompletoDeveCalcularTotalLiquidoComDescontos() {
        // Cenário: Sem Justa Causa, Salário de R$ 4.000, 1 dependente.
        CalculoRescisaoRequest req = CalculoRescisaoRequest.builder()
                .tipoRescisao(TipoRescisao.SEM_JUSTA_CAUSA)
                .salarioMensal(new BigDecimal("4000.00"))
                .dataAdmissao(LocalDate.of(2024, 1, 1))
                .dataDesligamento(LocalDate.of(2025, 8, 15)) // Saldo Salário: 2000.00
                .avisoIndenizado(false) // Meses no ano = 8
                .feriasVencidasDias(0)
                .saldoFgtsDepositado(new BigDecimal("5000.00"))
                .numeroDependentes(1)
                .build();

        // Verbas Brutas (Cálculos manuais para o teste)
        // Saldo Salário: 2000.00
        // 13º (8 meses): 2666.67
        // Férias Prop + 1/3 (8 meses): 3555.56
        // Saldo FGTS: 5000.00
        // Multa FGTS (40%): 2000.00
        // Total Bruto Esperado: 15222.23

        // Configurando o comportamento do mock de descontos com valores pré-calculados
        BigDecimal saldoSalarioBase = new BigDecimal("2000.00");
        BigDecimal decimoBase = new BigDecimal("2666.67");

        BigDecimal inssSobreSalario = new BigDecimal("157.50");
        BigDecimal inssSobre13 = new BigDecimal("232.50");
        BigDecimal irrfSobreSalario = new BigDecimal("0.00");
        BigDecimal irrfSobre13 = new BigDecimal("24.41");

        Mockito.when(descontosServiceMock.calcularInss(saldoSalarioBase)).thenReturn(inssSobreSalario);
        Mockito.when(descontosServiceMock.calcularInss(decimoBase)).thenReturn(inssSobre13);
        Mockito.when(descontosServiceMock.calcularIrrf(saldoSalarioBase, inssSobreSalario, 1)).thenReturn(irrfSobreSalario);
        Mockito.when(descontosServiceMock.calcularIrrf(decimoBase, inssSobre13, 1)).thenReturn(irrfSobre13);

        // Execução
        CalculoRescisaoResponse resp = service.calcular(req);

        // Validação
        BigDecimal totalDescontosEsperado = new BigDecimal("414.41"); // 157.50 + 232.50 + 0.00 + 24.41
        BigDecimal totalLiquidoEsperado = new BigDecimal("14807.82"); // 15222.23 - 414.41

        assertEquals(new BigDecimal("15222.23"), resp.getTotalBruto());
        assertEquals(totalDescontosEsperado, resp.getTotalDescontos());
        assertEquals(totalLiquidoEsperado, resp.getTotalLiquido());
    }
}