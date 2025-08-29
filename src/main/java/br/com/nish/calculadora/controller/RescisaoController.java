package br.com.nish.calculadora.controller;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.model.CalculoRescisao;
import br.com.nish.calculadora.model.CalculoRescisaoRepository;
import br.com.nish.calculadora.service.CalculoRescisaoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints relacionados a cálculo de rescisão.
 */
@RestController
@RequestMapping("/api/v1/rescisoes")
@RequiredArgsConstructor
@Tag(name = "Rescisões", description = "Operações de cálculo de verbas rescisórias")
public class RescisaoController {

    private final CalculoRescisaoService calculoRescisaoService;
    private final CalculoRescisaoRepository calculoRescisaoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Calcula as verbas e persiste o resultado.
     * Enquanto o auth não está pronto, usa usuarioId temporário = 1.
     * @param request dados de entrada do cálculo
     * @return resultado do cálculo com breakdown
     */
    @PostMapping("/calcular")
    @Operation(summary = "Calcular rescisão", description = "Calcula e salva o detalhamento das verbas")
    public ResponseEntity<CalculoRescisaoResponse> calcular(@Valid @RequestBody CalculoRescisaoRequest request)
            throws JsonProcessingException {

        CalculoRescisaoResponse response = calculoRescisaoService.calcular(request);

        CalculoRescisao entity = CalculoRescisao.builder()
                .usuarioId(1L) // TODO: substituir pelo id do usuário autenticado quando JWT estiver pronto
                .tipoRescisao(request.getTipoRescisao().name())
                .salarioMensal(request.getSalarioMensal())
                .dataAdmissao(request.getDataAdmissao())
                .dataDesligamento(request.getDataDesligamento())
                .avisoIndenizado(request.isAvisoIndenizado())
                .feriasVencidasDias(request.getFeriasVencidasDias())
                .mesesTrabalhadosNoAnoAtual(request.getMesesTrabalhadosNoAnoAtual())
                .saldoFgtsDepositado(request.getSaldoFgtsDepositado())
                .totalBruto(response.getTotalBruto())
                .totalDescontos(response.getTotalDescontos())
                .totalLiquido(response.getTotalLiquido())
                .pagamentoAte(response.getPagamentoAte())
                .componentesJson(objectMapper.writeValueAsString(response.getComponentes()))
                .build();

        calculoRescisaoRepository.save(entity);

        return ResponseEntity.ok(response);
    }
}
