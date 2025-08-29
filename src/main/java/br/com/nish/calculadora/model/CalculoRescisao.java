package br.com.nish.calculadora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade que persiste um cálculo de rescisão e seu resultado.
 */
@Entity
@Table(name = "calculos_rescisao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoRescisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "tipo_rescisao", nullable = false)
    private String tipoRescisao;

    @Column(name = "salario_mensal", nullable = false)
    private BigDecimal salarioMensal;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Column(name = "data_desligamento", nullable = false)
    private LocalDate dataDesligamento;

    @Column(name = "aviso_indenizado", nullable = false)
    private boolean avisoIndenizado;

    @Column(name = "ferias_vencidas_dias", nullable = false)
    private int feriasVencidasDias;

    @Column(name = "meses_trabalhados_ano", nullable = false)
    private int mesesTrabalhadosNoAnoAtual;

    @Column(name = "saldo_fgts_depositado", nullable = false)
    private BigDecimal saldoFgtsDepositado;

    @Column(name = "total_bruto", nullable = false)
    private BigDecimal totalBruto;

    @Column(name = "total_descontos", nullable = false)
    private BigDecimal totalDescontos;

    @Column(name = "total_liquido", nullable = false)
    private BigDecimal totalLiquido;

    /**
     * JSON com a lista de componentes do cálculo.
     */
    @Column(name = "componentes", columnDefinition = "jsonb", nullable = false)
    private String componentesJson;

    @Column(name = "pagamento_ate")
    private LocalDate pagamentoAte;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
