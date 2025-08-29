package br.com.nish.calculadora.controller;

import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.service.CalculoRescisaoService;
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

    /**
     * Calcula as verbas rescisórias para o payload informado.
     * @param request dados de entrada do cálculo
     * @return resultado do cálculo com breakdown
     */
    @PostMapping("/calcular")
    @Operation(summary = "Calcular rescisão", description = "Recebe dados do contrato e retorna o detalhamento das verbas calculadas")
    public ResponseEntity<CalculoRescisaoResponse> calcular(@Valid @RequestBody CalculoRescisaoRequest request) {
        CalculoRescisaoResponse response = calculoRescisaoService.calcular(request);
        return ResponseEntity.ok(response);
    }
}
