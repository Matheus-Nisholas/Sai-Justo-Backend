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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * @throws JsonProcessingException erro ao serializar componentes
     */
    @PostMapping("/calcular")
    @Operation(summary = "Calcular rescisão", description = "Calcula e salva o detalhamento das verbas")
    public ResponseEntity<CalculoRescisaoResponse> calcular(
            @Valid @RequestBody CalculoRescisaoRequest request
    ) throws JsonProcessingException {

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

    /**
     * Lista o histórico paginado de cálculos do usuário.
     * Enquanto não há auth, filtra por usuarioId = 1.
     * @param page número da página, começando em 0
     * @param size tamanho da página
     * @return página com cálculos mais recentes primeiro
     */
    @GetMapping("/historico")
    @Operation(summary = "Histórico de cálculos", description = "Retorna cálculos paginados do usuário atual")
    public ResponseEntity<Page<CalculoRescisao>> historico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CalculoRescisao> result = calculoRescisaoRepository
                .findByUsuarioIdOrderByCriadoEmDesc(1L, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * Busca um cálculo específico por id.
     * Observação: quando o auth estiver pronto, validar o proprietário.
     * @param id identificador do cálculo
     * @return cálculo se existir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obter cálculo por id", description = "Retorna um cálculo específico")
    public ResponseEntity<CalculoRescisao> obterPorId(@PathVariable Long id) {
        return calculoRescisaoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
