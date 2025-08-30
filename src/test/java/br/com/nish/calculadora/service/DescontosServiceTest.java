package br.com.nish.calculadora.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescontosServiceTest {

    private DescontosService descontosService;

    @BeforeEach
    void setUp() {
        descontosService = new DescontosService();
    }

    @Test
    @DisplayName("Deve calcular INSS na primeira faixa (7.5%)")
    void deveCalcularInssPrimeiraFaixa() {
        // Salário: 1500.00 * 7.5% = 112.50
        BigDecimal baseCalculo = new BigDecimal("1500.00");
        BigDecimal expected = new BigDecimal("112.50");
        assertEquals(expected, descontosService.calcularInss(baseCalculo));
    }

    @Test
    @DisplayName("Deve calcular INSS em múltiplas faixas (até 9%)")
    void deveCalcularInssMultiplasFaixas() {
        // Salário: 2000.00
        // Faixa 1: 1500.00 * 7.5% = 112.50
        // Faixa 2: (2000.00 - 1500.00) * 9% = 500.00 * 0.09 = 45.00
        // Total: 112.50 + 45.00 = 157.50
        BigDecimal baseCalculo = new BigDecimal("2000.00");
        BigDecimal expected = new BigDecimal("157.50");
        assertEquals(expected, descontosService.calcularInss(baseCalculo));
    }

    @Test
    @DisplayName("Deve retornar zero de IRRF para base isenta")
    void deveRetornarZeroIrrfBaseIsenta() {
        BigDecimal baseTributavel = new BigDecimal("2200.00");
        BigDecimal inss = new BigDecimal("175.50"); // Valor de exemplo
        int dependentes = 0;
        // Base de cálculo = 2200 - 175.50 = 2024.50 (isento)
        BigDecimal expected = new BigDecimal("0.00");
        assertEquals(expected, descontosService.calcularIrrf(baseTributavel, inss, dependentes));
    }

    @Test
    @DisplayName("Deve calcular IRRF com dedução por dependentes")
    void deveCalcularIrrfComDependentes() {
        // Salário: 3500.00
        BigDecimal baseTributavel = new BigDecimal("3500.00");
        // INSS sobre 3500: (1500*0.075) + (1300*0.09) + (700*0.12) = 112.5 + 117 + 84 = 313.50
        BigDecimal inss = new BigDecimal("313.50");
        int dependentes = 2; // Dedução: 2 * 189.59 = 379.18
        // Base IRRF = 3500 - 313.50 - 379.18 = 2807.32
        // Faixa IRRF: 7.5% -> (2807.32 * 0.075) - 169.44 = 210.55 - 169.44 = 41.11
        BigDecimal expected = new BigDecimal("41.11");
        assertEquals(expected, descontosService.calcularIrrf(baseTributavel, inss, dependentes));
    }

    @Test
    @DisplayName("Deve calcular IRRF na alíquota máxima")
    void deveCalcularIrrfAliquotaMaxima() {
        // Salário: 6000.00
        BigDecimal baseTributavel = new BigDecimal("6000.00");
        // INSS sobre 6000: (1500*.075)+(1300*.09)+(1400*.12)+(1800*.14) = 112.5+117+168+252 = 649.50
        BigDecimal inss = new BigDecimal("649.50");
        int dependentes = 1; // Dedução: 189.59
        // Base IRRF = 6000 - 649.50 - 189.59 = 5160.91
        // Faixa IRRF: 27.5% -> (5160.91 * 0.275) - 896.00 = 1419.25 - 896.00 = 523.25
        BigDecimal expected = new BigDecimal("523.25");
        assertEquals(expected, descontosService.calcularIrrf(baseTributavel, inss, dependentes));
    }
}