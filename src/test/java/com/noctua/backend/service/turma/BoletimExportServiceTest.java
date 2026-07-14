package com.noctua.backend.service.turma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.entity.Nota.NotaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.enums.TipoAvaliacao;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.AvaliacaoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;
import com.noctua.backend.repository.turma.NotaRepository;
import com.noctua.backend.repository.turma.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class BoletimExportServiceTest {

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private FrequenciaRepository frequenciaRepository;

    @InjectMocks
    private BoletimExportService boletimExportService;

    @Test
    void exportarBoletimAnualDeveGerarPlanilhaComMediasEFaltas() throws Exception {
        TurmaEntity turma = criarTurma(10L, 3, 20, "Matematica");
        AlunoEntity ana = criarAluno(1L, "Ana", turma);
        AlunoEntity bia = criarAluno(2L, "Bia", turma);
        AvaliacaoEntity provaP1 = criarAvaliacao(100L, turma, 1, 2, TipoAvaliacao.PROVA);
        AvaliacaoEntity trabalhoP1 = criarAvaliacao(101L, turma, 1, 1, TipoAvaliacao.TRABALHO);
        AvaliacaoEntity provaP2 = criarAvaliacao(102L, turma, 2, 1, TipoAvaliacao.PROVA);

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(bia, ana));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(provaP1, trabalhoP1, provaP2));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(
                criarNota(1L, provaP1, ana, "8.00", false),
                criarNota(2L, trabalhoP1, ana, "10.00", false),
                criarNota(3L, provaP2, ana, "7.00", false),
                criarNota(4L, provaP1, bia, null, true)));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(1L)).thenReturn(List.of(
                criarFrequencia(ana, 1, 2),
                criarFrequencia(ana, 2, null)));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(2L)).thenReturn(List.of());

        byte[] arquivo = boletimExportService.exportarBoletimAnual(10L);

        assertTrue(arquivo.length > 0);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheet("Boletim Anual");

            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals("Ana", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals(8.67, sheet.getRow(3).getCell(1).getNumericCellValue(), 0.01);
            assertEquals(2, (int) sheet.getRow(3).getCell(2).getNumericCellValue());
            assertEquals(7.00, sheet.getRow(3).getCell(3).getNumericCellValue(), 0.01);
            assertEquals(1, (int) sheet.getRow(3).getCell(4).getNumericCellValue());
            assertEquals("Bia", sheet.getRow(4).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportarBoletimPeriodoDeveUsarNotaDaSegundaChamadaEFrequenciaFormatada() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, null);
        AlunoEntity ana = criarAluno(1L, "Ana", turma);
        AvaliacaoEntity prova = criarAvaliacao(100L, turma, 1, 2, TipoAvaliacao.PROVA);
        AvaliacaoEntity segundaChamada = criarAvaliacao(101L, turma, 1, 2, TipoAvaliacao.PROVA);
        segundaChamada.setAvaliacaoPai(prova);
        AvaliacaoEntity trabalho = criarAvaliacao(102L, turma, 1, 1, TipoAvaliacao.TRABALHO);

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(ana));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(trabalho, segundaChamada, prova));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(
                criarNota(1L, prova, ana, null, true),
                criarNota(2L, segundaChamada, ana, "9.00", false),
                criarNota(3L, trabalho, ana, null, false)));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(1L)).thenReturn(List.of(
                criarFrequencia(ana, 1, 3),
                criarFrequencia(ana, 2, 5)));

        byte[] arquivo = boletimExportService.exportarBoletimPeriodo(10L, 1);

        assertTrue(arquivo.length > 0);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet resumoSheet = workbook.getSheetAt(0);
            Sheet mediasSheet = workbook.getSheet("Detalhamento de médias");

            assertTrue(resumoSheet.getSheetName().contains("Bimestre"));
            assertEquals("Ana", resumoSheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(9.00, resumoSheet.getRow(2).getCell(1).getNumericCellValue(), 0.01);
            assertEquals("85,0%", resumoSheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals(9.00, mediasSheet.getRow(2).getCell(1).getNumericCellValue(), 0.01);
            assertEquals("-", mediasSheet.getRow(2).getCell(2).getStringCellValue());
        }
    }

    @Test
    void exportarBoletimPeriodoDeveUsarTracoQuandoNaoHouverAulasPrevistas() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 0, "Historia");
        AlunoEntity ana = criarAluno(1L, "Ana", turma);

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(ana));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of());
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of());
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(1L)).thenReturn(List.of());

        byte[] arquivo = boletimExportService.exportarBoletimPeriodo(10L, 1);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            assertEquals("-", workbook.getSheetAt(0).getRow(2).getCell(2).getStringCellValue());
        }
    }

    @Test
    void exportarBoletimAnualDeveLancarNotFoundQuandoTurmaNaoExistir() {
        when(turmaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boletimExportService.exportarBoletimAnual(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void exportarBoletimPeriodoDeveUsarCabecalhoDescritivoEAlinharAlunoAEsquerda() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, "Matematica");
        AlunoEntity aluno = criarAluno(100L, "Ana Silva", turma);
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, turma, 1, 2, TipoAvaliacao.PROVA, "Algebra");
        NotaEntity nota = criarNota(1L, avaliacao, aluno, "8.50", false);
        FrequenciaEntity primeiraFalta = criarFrequencia(aluno, 1L, 1, 2, LocalDateTime.of(2026, 4, 15, 8, 0));
        FrequenciaEntity segundaFalta = criarFrequencia(aluno, 2L, 1, 1, LocalDateTime.of(2026, 4, 16, 8, 0));

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(avaliacao));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(nota));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(primeiraFalta, segundaFalta));

        byte[] bytes = boletimExportService.exportarBoletimPeriodo(10L, 1);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Boletim 1º Bimestre - Turma A | Matematica", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Média", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Frequência", sheet.getRow(1).getCell(2).getStringCellValue());
            assertTrue(sheet.getRow(1).getCell(1).getCellStyle().getWrapText());
            assertEquals("Ana Silva", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(8.5, sheet.getRow(2).getCell(1).getNumericCellValue());
            assertEquals("85,0%", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals(HorizontalAlignment.LEFT, sheet.getRow(2).getCell(0).getCellStyle().getAlignment());

            Sheet mediasSheet = workbook.getSheet("Detalhamento de médias");
            assertEquals("Avaliações do 1º bimestre - Turma A | Matematica", mediasSheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Aluno", mediasSheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("AV1 - Prova: Algebra (P2)", mediasSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Ana Silva", mediasSheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(8.5, mediasSheet.getRow(2).getCell(1).getNumericCellValue());

            Sheet faltasSheet = workbook.getSheet("Detalhamento de faltas");
            assertEquals("Faltas do 1º bimestre - Turma A | Matematica", faltasSheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Aluno", faltasSheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Bimestre, data e períodos faltados", faltasSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Faltas totais", faltasSheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("Ana Silva", faltasSheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("15/04/2026 - 2 faltas", faltasSheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals(3, (int) faltasSheet.getRow(2).getCell(2).getNumericCellValue());
            assertEquals("16/04/2026 - 1 falta", faltasSheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals(3, faltasSheet.getNumMergedRegions());
        }
    }

    @Test
    void exportarBoletimAnualDeveUsarCabecalhosPorExtensoParaMediaEFaltas() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, "Matematica");
        AlunoEntity aluno = criarAluno(100L, "Ana Silva", turma);
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, turma, 1, 2, TipoAvaliacao.PROVA, "Algebra");
        NotaEntity nota = criarNota(1L, avaliacao, aluno, "8.50", false);
        FrequenciaEntity falta = criarFrequencia(aluno, 1L, 1, 2, LocalDateTime.of(2026, 4, 15, 8, 0));
        FrequenciaEntity segundaFalta = criarFrequencia(aluno, 2L, 2, 3, LocalDateTime.of(2026, 8, 20, 8, 0));

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(avaliacao));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(nota));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(falta, segundaFalta));

        byte[] bytes = boletimExportService.exportarBoletimAnual(10L);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Boletim Anual");

            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals("Média", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("Faltas", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("Final", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("Média", sheet.getRow(2).getCell(9).getStringCellValue());
            assertEquals("Faltas", sheet.getRow(2).getCell(10).getStringCellValue());

            Sheet mediasSheet = workbook.getSheet("Detalhamento de médias");
            assertEquals("Avaliações do ano - Turma A | Matematica", mediasSheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Aluno", mediasSheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("1º Bimestre - AV1\nProva: Algebra (P2)", mediasSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Ana Silva", mediasSheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(8.5, mediasSheet.getRow(2).getCell(1).getNumericCellValue());

            Sheet faltasSheet = workbook.getSheet("Detalhamento de faltas");
            assertEquals("Faltas do ano - Turma A | Matematica", faltasSheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Bimestre, data e períodos faltados", faltasSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("1º Bimestre - 15/04/2026 - 2 faltas", faltasSheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("2º Bimestre - 20/08/2026 - 3 faltas", faltasSheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals(5, (int) faltasSheet.getRow(2).getCell(2).getNumericCellValue());
        }
    }

    @Test
    void exportarBoletimPeriodoPdfDeveGerarArquivoPdf() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, "Matematica");
        AlunoEntity aluno = criarAluno(100L, "Ana Silva", turma);
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, turma, 1, 2, TipoAvaliacao.PROVA, "Algebra");
        NotaEntity nota = criarNota(1L, avaliacao, aluno, "8.50", false);
        FrequenciaEntity primeiraFalta = criarFrequencia(aluno, 1L, 1, 2, LocalDateTime.of(2026, 4, 15, 8, 0));
        FrequenciaEntity segundaFalta = criarFrequencia(aluno, 2L, 1, 1, LocalDateTime.of(2026, 4, 16, 8, 0));

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(avaliacao));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(nota));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(primeiraFalta, segundaFalta));

        byte[] pdf = boletimExportService.exportarBoletimPeriodoPdf(10L, 1);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Data e períodos faltados"));
        }
    }

    @Test
    void exportarBoletimAnualPdfDeveGerarArquivoPdf() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, "Matematica");
        AlunoEntity aluno = criarAluno(100L, "Ana Silva", turma);
        AvaliacaoEntity avaliacaoP1 = criarAvaliacao(50L, turma, 1, 2, TipoAvaliacao.PROVA, "Algebra");
        AvaliacaoEntity avaliacaoP2 = criarAvaliacao(51L, turma, 2, 1, TipoAvaliacao.TRABALHO, "Geometria");
        NotaEntity notaP1 = criarNota(1L, avaliacaoP1, aluno, "8.50", false);
        NotaEntity notaP2 = criarNota(2L, avaliacaoP2, aluno, "7.00", false);
        FrequenciaEntity faltaP1 = criarFrequencia(aluno, 1L, 1, 2, LocalDateTime.of(2026, 4, 15, 8, 0));
        FrequenciaEntity faltaP2 = criarFrequencia(aluno, 2L, 2, 3, LocalDateTime.of(2026, 8, 20, 8, 0));

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(avaliacaoP1, avaliacaoP2));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(notaP1, notaP2));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(faltaP1, faltaP2));

        byte[] pdf = boletimExportService.exportarBoletimAnualPdf(10L);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("1º BI - 15/04/2026 - 2 faltas"));
        }
    }

    @Test
    void exportarBoletimAnualPdfDeveUsarPaisagemQuandoTabelaForLarga() throws Exception {
        TurmaEntity turma = criarTurma(10L, 4, 20, "Matematica");
        AlunoEntity aluno = criarAluno(100L, "Ana Silva", turma);
        AvaliacaoEntity avaliacaoP1A = criarAvaliacao(50L, turma, 1, 2, TipoAvaliacao.PROVA, "Algebra");
        AvaliacaoEntity avaliacaoP1B = criarAvaliacao(51L, turma, 1, 1, TipoAvaliacao.TRABALHO, "Geometria");
        AvaliacaoEntity avaliacaoP2A = criarAvaliacao(52L, turma, 2, 2, TipoAvaliacao.PROVA, "Funcoes");
        AvaliacaoEntity avaliacaoP2B = criarAvaliacao(53L, turma, 2, 1, TipoAvaliacao.ATIVIDADE, "Graficos");
        AvaliacaoEntity avaliacaoP3A = criarAvaliacao(54L, turma, 3, 2, TipoAvaliacao.PROVA, "Trigonometria");
        AvaliacaoEntity avaliacaoP3B = criarAvaliacao(55L, turma, 3, 1, TipoAvaliacao.TRABALHO, "Estatistica");
        AvaliacaoEntity avaliacaoP4A = criarAvaliacao(56L, turma, 4, 2, TipoAvaliacao.PROVA, "Probabilidade");
        AvaliacaoEntity avaliacaoP4B = criarAvaliacao(57L, turma, 4, 1, TipoAvaliacao.ATIVIDADE, "Revisao");
        AvaliacaoEntity avaliacaoP1C = criarAvaliacao(58L, turma, 1, 1, TipoAvaliacao.ATIVIDADE, "Equacoes");
        AvaliacaoEntity avaliacaoP1D = criarAvaliacao(59L, turma, 1, 1, TipoAvaliacao.TRABALHO, "Problemas");
        AvaliacaoEntity avaliacaoP2C = criarAvaliacao(60L, turma, 2, 1, TipoAvaliacao.ATIVIDADE, "Polinomios");
        AvaliacaoEntity avaliacaoP2D = criarAvaliacao(61L, turma, 2, 1, TipoAvaliacao.TRABALHO, "Sistemas");
        AvaliacaoEntity avaliacaoP3C = criarAvaliacao(62L, turma, 3, 1, TipoAvaliacao.ATIVIDADE, "Razoes");
        AvaliacaoEntity avaliacaoP3D = criarAvaliacao(63L, turma, 3, 1, TipoAvaliacao.TRABALHO, "Sequencias");
        AvaliacaoEntity avaliacaoP4C = criarAvaliacao(64L, turma, 4, 1, TipoAvaliacao.ATIVIDADE, "Combinatoria");
        AvaliacaoEntity avaliacaoP4D = criarAvaliacao(65L, turma, 4, 1, TipoAvaliacao.TRABALHO, "Simulado");

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(
                avaliacaoP1A, avaliacaoP1B, avaliacaoP2A, avaliacaoP2B,
                avaliacaoP3A, avaliacaoP3B, avaliacaoP4A, avaliacaoP4B,
                avaliacaoP1C, avaliacaoP1D, avaliacaoP2C, avaliacaoP2D,
                avaliacaoP3C, avaliacaoP3D, avaliacaoP4C, avaliacaoP4D));
        when(notaRepository.findByAvaliacao_TurmaId(10L)).thenReturn(List.of(
                criarNota(1L, avaliacaoP1A, aluno, "8.50", false),
                criarNota(2L, avaliacaoP1B, aluno, "9.00", false),
                criarNota(3L, avaliacaoP2A, aluno, "7.50", false),
                criarNota(4L, avaliacaoP2B, aluno, "8.00", false),
                criarNota(5L, avaliacaoP3A, aluno, "9.50", false),
                criarNota(6L, avaliacaoP3B, aluno, "8.00", false),
                criarNota(7L, avaliacaoP4A, aluno, "7.00", false),
                criarNota(8L, avaliacaoP4B, aluno, "8.50", false),
                criarNota(9L, avaliacaoP1C, aluno, "8.00", false),
                criarNota(10L, avaliacaoP1D, aluno, "7.50", false),
                criarNota(11L, avaliacaoP2C, aluno, "9.00", false),
                criarNota(12L, avaliacaoP2D, aluno, "8.50", false),
                criarNota(13L, avaliacaoP3C, aluno, "7.00", false),
                criarNota(14L, avaliacaoP3D, aluno, "8.00", false),
                criarNota(15L, avaliacaoP4C, aluno, "9.00", false),
                criarNota(16L, avaliacaoP4D, aluno, "8.50", false)));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of());

        byte[] pdf = boletimExportService.exportarBoletimAnualPdf(10L);

        try (PDDocument document = PDDocument.load(pdf)) {
            boolean possuiPaginaPaisagem = false;
            for (PDPage page : document.getPages()) {
                if (page.getMediaBox().getWidth() > page.getMediaBox().getHeight()) {
                    possuiPaginaPaisagem = true;
                    break;
                }
            }
            assertTrue(possuiPaginaPaisagem);
            assertTrue(document.getNumberOfPages() > 3);
        }
    }

    @Test
    void gerarNomeArquivoBoletimDeveCriarNomeAmigavel() {
        TurmaEntity turma = criarTurma(10L, 3, 20, "Matematica");
        turma.setNome("3º Ano A - Manhã");

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));

        assertEquals("boletim-anual-3º Ano A - Manhã.pdf",
                boletimExportService.gerarNomeArquivoBoletim(10L, null, ".pdf"));
        assertEquals("boletim-2-trimestre-3º Ano A - Manhã.xlsx",
                boletimExportService.gerarNomeArquivoBoletim(10L, 2, "xlsx"));
    }

    private TurmaEntity criarTurma(Long id, Integer qtdePeriodos, Integer qtdeAulasPrevistasPeriodo, String disciplina) {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(id);
        turma.setNome("Turma A");
        turma.setAnoLetivo(LocalDate.of(2026, 1, 1));
        turma.setQtdePeriodos(qtdePeriodos);
        turma.setQtdeAulasPrevistasPeriodo(qtdeAulasPrevistasPeriodo);
        turma.setDisciplina(disciplina);
        turma.setTurno(Turno.MATUTINO);
        turma.setMediaMinima(6.0);
        turma.setAtivo(true);
        return turma;
    }

    private AlunoEntity criarAluno(Long id, String nome, TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(id);
        aluno.setNome(nome);
        aluno.setAtivo(true);
        aluno.setTurma(turma);
        return aluno;
    }

    private AvaliacaoEntity criarAvaliacao(Long id, TurmaEntity turma, Integer periodo, Integer peso, TipoAvaliacao tipo) {
        return criarAvaliacao(id, turma, periodo, peso, tipo, "Avaliacao " + id);
    }

    private AvaliacaoEntity criarAvaliacao(
            Long id,
            TurmaEntity turma,
            Integer periodo,
            Integer peso,
            TipoAvaliacao tipo,
            String tema) {

        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        avaliacao.setId(id);
        avaliacao.setTema(tema);
        avaliacao.setData(LocalDateTime.of(2026, 5, id.intValue() % 20 + 1, 8, 0));
        avaliacao.setPeso(peso);
        avaliacao.setTipo(tipo);
        avaliacao.setPeriodo(periodo);
        avaliacao.setTurma(turma);
        avaliacao.setNumeroChamada(1);
        avaliacao.setConcluida(true);
        return avaliacao;
    }

    private NotaEntity criarNota(Long id, AvaliacaoEntity avaliacao, AlunoEntity aluno, String valor, Boolean naoRealizada) {
        NotaEntity nota = new NotaEntity();
        nota.setId(id);
        nota.setAvaliacao(avaliacao);
        nota.setAluno(aluno);
        nota.setValor(valor != null ? new BigDecimal(valor) : null);
        nota.setNaoRealizada(naoRealizada);
        return nota;
    }

    private FrequenciaEntity criarFrequencia(AlunoEntity aluno, Integer periodo, Integer periodosFaltados) {
        return criarFrequencia(aluno, 1L, periodo, periodosFaltados, LocalDateTime.of(2026, 5, 10, 8, 0));
    }

    private FrequenciaEntity criarFrequencia(
            AlunoEntity aluno,
            Long id,
            Integer periodo,
            Integer periodosFaltados,
            LocalDateTime dataFalta) {

        FrequenciaEntity frequencia = new FrequenciaEntity();
        frequencia.setId(id);
        frequencia.setAluno(aluno);
        frequencia.setPeriodo(periodo);
        frequencia.setPeriodosFaltados(periodosFaltados);
        frequencia.setAtivo(true);
        frequencia.setDataFalta(dataFalta);
        return frequencia;
    }
}
