package br.com.nish.calculadora.controller;

import br.com.nish.calculadora.auth.Usuario;
import br.com.nish.calculadora.auth.UsuarioRepository;
import br.com.nish.calculadora.dto.CalculoRescisaoRequest;
import br.com.nish.calculadora.dto.CalculoRescisaoResponse;
import br.com.nish.calculadora.model.CalculoRescisao;
import br.com.nish.calculadora.model.CalculoRescisaoRepository;
import br.com.nish.calculadora.service.CalculoRescisaoService;
import br.com.nish.calculadora.service.PdfGenerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rescisoes")
@RequiredArgsConstructor
@Tag(name = "Rescisões", description = "Operações de cálculo de verbas rescisórias")
public class RescisaoController {

    private final CalculoRescisaoService calculoRescisaoService;
    private final CalculoRescisaoRepository calculoRescisaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final PdfGenerationService pdfGenerationService;

    @PostMapping("/calcular")
    @Operation(summary = "Calcular rescisão", description = "Calcula e salva o detalhamento das verbas")
    public ResponseEntity<CalculoRescisaoResponse> calcular(
            @Valid @RequestBody CalculoRescisaoRequest request
    ) throws JsonProcessingException {

        Long userId = getAuthenticatedUserId().orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));

        CalculoRescisaoResponse response = calculoRescisaoService.calcular(request);
        CalculoRescisao entity = CalculoRescisao.builder()
                .usuarioId(userId)
                .nomeEmpregado(request.getNomeEmpregado())
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

    @GetMapping("/historico")
    @Operation(summary = "Histórico de cálculos", description = "Retorna cálculos paginados do usuário atual")
    public ResponseEntity<Page<CalculoRescisao>> historico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getAuthenticatedUserId().orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));
        Page<CalculoRescisao> result = calculoRescisaoRepository
                .findByUsuarioIdOrderByCriadoEmDesc(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter cálculo por id", description = "Retorna um cálculo específico do usuário")
    public ResponseEntity<CalculoRescisao> obterPorId(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId().orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));
        return calculoRescisaoRepository.findById(id)
                .filter(c -> c.getUsuarioId().equals(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cálculo", description = "Remove um cálculo do histórico do usuário")
    public ResponseEntity<Void> excluirCalculo(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId().orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));

        return calculoRescisaoRepository.findById(id)
                .map(calculo -> {
                    if (!calculo.getUsuarioId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
                    calculoRescisaoRepository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.<Void>notFound().build());
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar cálculo para PDF", description = "Gera um recibo em PDF de um cálculo histórico")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id) {
        Long userId = getAuthenticatedUserId().orElseThrow(() -> new IllegalStateException("Usuário não autenticado"));

        return calculoRescisaoRepository.findById(id)
                .filter(calculo -> calculo.getUsuarioId().equals(userId))
                .map(calculo -> {
                    try {
                        ByteArrayInputStream pdfStream = pdfGenerationService.gerarReciboRescisao(calculo);
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Content-Disposition", "inline; filename=recibo_rescisao_" + id + ".pdf");

                        return ResponseEntity
                                .ok()
                                .headers(headers)
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdfStream.readAllBytes());
                    } catch (IOException | DocumentException e) {
                        e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<byte[]>build();
                    }
                })
                // ALTERADO: Adicionamos <byte[]> para corrigir o erro de compilação.
                .orElse(ResponseEntity.<byte[]>notFound().build());
    }

    private Optional<Long> getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Optional.empty();
        }
        String email = auth.getName();
        return usuarioRepository.findByEmail(email).map(Usuario::getId);
    }
}