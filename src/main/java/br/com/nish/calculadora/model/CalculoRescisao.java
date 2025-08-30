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
// NOVO: Importação da anotação
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


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

    // ... (outros campos permanecem os mesmos) ...
    private Long usuarioId;
    private String tipoRescisao;
    private BigDecimal salarioMensal;
    private LocalDate dataAdmissao;
    private LocalDate dataDesligamento;
    private boolean avisoIndenizado;
    private int feriasVencidasDias;
    @Column(name = "meses_trabalhados_ano") // Corrigindo o nome da coluna para corresponder ao V2.sql
    private int mesesTrabalhadosNoAnoAtual;
    private BigDecimal saldoFgtsDepositado;
    private BigDecimal totalBruto;
    private BigDecimal totalDescontos;
    private BigDecimal totalLiquido;
    private LocalDate pagamentoAte;

    /**
     * JSON com a lista de componentes do cálculo.
     */
    // ALTERADO: Adicionamos @JdbcTypeCode(SqlTypes.JSON) para dizer ao Hibernate
    // explicitamente para tratar este campo String como um tipo JSON no banco de dados.
    // Isso resolve a incompatibilidade entre o H2 e o PostgreSQL.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "componentes", columnDefinition = "jsonb", nullable = false)
    private String componentesJson;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}