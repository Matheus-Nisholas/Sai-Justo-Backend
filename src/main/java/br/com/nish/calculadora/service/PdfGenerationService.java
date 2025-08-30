package br.com.nish.calculadora.service;

import br.com.nish.calculadora.dto.Componente;
import br.com.nish.calculadora.model.CalculoRescisao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PdfGenerationService {

    private final ObjectMapper objectMapper;

    public PdfGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ByteArrayInputStream gerarReciboRescisao(CalculoRescisao calculo) throws IOException, DocumentException {
        // Usa um fluxo de bytes em memória para construir o PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();

        // --- Título ---
        Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph titulo = new Paragraph("Termo de Rescisão do Contrato de Trabalho", fontTitulo);
        titulo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" ")); // Linha em branco

        // --- Dados do Cálculo ---
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        document.add(new Paragraph("Nome do Empregado: " + (calculo.getNomeEmpregado() != null ? calculo.getNomeEmpregado() : "N/A")));
        document.add(new Paragraph("Data de Admissão: " + calculo.getDataAdmissao().format(formatter)));
        document.add(new Paragraph("Data de Desligamento: " + calculo.getDataDesligamento().format(formatter)));
        document.add(new Paragraph(" "));

        // --- Tabela de Verbas Rescisórias (Proventos) ---
        document.add(new Paragraph("Verbas Rescisórias (Proventos)", new Font(Font.HELVETICA, 14, Font.BOLD)));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{3, 1});

        // Cabeçalho da tabela
        Font headFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        PdfPCell hcell;
        hcell = new PdfPCell(new Phrase("Descrição", headFont));
        hcell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Phrase("Valor (R$)", headFont));
        hcell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(hcell);

        // Corpo da tabela
        List<Componente> componentes = objectMapper.readValue(calculo.getComponentesJson(), new TypeReference<>() {});
        for (Componente comp : componentes) {
            table.addCell(comp.getNome());
            PdfPCell valorCell = new PdfPCell(new Phrase(formatCurrency(comp.getValor())));
            valorCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            table.addCell(valorCell);
        }
        document.add(table);
        document.add(new Paragraph(" "));

        // --- Totais ---
        document.add(new Paragraph("Total Bruto: " + formatCurrency(calculo.getTotalBruto())));
        document.add(new Paragraph("Total de Descontos: " + formatCurrency(calculo.getTotalDescontos())));

        Paragraph totalLiquido = new Paragraph("Total Líquido a Receber: " + formatCurrency(calculo.getTotalLiquido()), new Font(Font.HELVETICA, 12, Font.BOLD));
        totalLiquido.setAlignment(Paragraph.ALIGN_RIGHT);
        document.add(totalLiquido);

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String formatCurrency(BigDecimal value) {
        Locale ptBr = new Locale("pt", "BR");
        return NumberFormat.getCurrencyInstance(ptBr).format(value);
    }
}