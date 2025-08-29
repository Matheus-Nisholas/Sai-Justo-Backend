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
 * Implementação MVP com valores simulados para validar o fluxo.
 */
@Service
public class CalculoRescisaoService {

    private static final BigDecimal TRES = new BigDecimal("3");

    /**
     * Executa um cálculo mock usando o salário mensal como base.
     * Substituir por fórmulas reais nas próximas iterações.
     *
     * @param req payload de entrada
     * @return resposta com breakdown e totais
     */
    public CalculoRescisaoResponse calcular(CalculoRescisaoRequest req) {
        BigDecimal base = req.getSalarioMensal();
        BigDecimal parcela = base.divide(TRES, 2, RoundingMode.HALF_UP);

        List<Componente> componentes = new ArrayList<Componente>();
        componentes.add(new Componente("Saldo de salário", parcela));
        componentes.add(new Componente("13º proporcional", parcela));
        componentes.add(new Componente("Férias + 1/3", parcela));

        BigDecimal totalBruto = parcela.multiply(TRES).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDescontos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLiquido = totalBruto.subtract(totalDescontos).setScale(2, RoundingMode.HALF_UP);

        CalculoRescisaoResponse response = CalculoRescisaoResponse.builder()
                .componentes(componentes)
                .totalBruto(totalBruto)
                .totalDescontos(totalDescontos)
                .totalLiquido(totalLiquido)
                .pagamentoAte(LocalDate.now().plusDays(10))
                .build();

        return response;
    }
}
