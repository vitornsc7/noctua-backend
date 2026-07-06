package com.noctua.backend.service.turma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            assertEquals("Boletim Anual", workbook.getSheetAt(0).getSheetName());
            assertEquals("Ana", workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue());
            assertEquals(8.67, workbook.getSheetAt(0).getRow(3).getCell(1).getNumericCellValue(), 0.01);
            assertEquals(2, (int) workbook.getSheetAt(0).getRow(3).getCell(2).getNumericCellValue());
            assertEquals(7.00, workbook.getSheetAt(0).getRow(3).getCell(3).getNumericCellValue(), 0.01);
            assertEquals(1, (int) workbook.getSheetAt(0).getRow(3).getCell(4).getNumericCellValue());
            assertEquals("Bia", workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
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
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            assertTrue(workbook.getSheetAt(0).getSheetName().contains("Bimestre"));
            assertEquals("Ana", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
            assertEquals(9.00, workbook.getSheetAt(0).getRow(2).getCell(1).getNumericCellValue(), 0.01);
            assertEquals("-", workbook.getSheetAt(0).getRow(2).getCell(2).getStringCellValue());
            assertEquals(9.00, workbook.getSheetAt(0).getRow(2).getCell(3).getNumericCellValue(), 0.01);
            assertEquals("85,0%", workbook.getSheetAt(0).getRow(2).getCell(4).getStringCellValue());
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

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            Cell frequencia = workbook.getSheetAt(0).getRow(2).getCell(2);
            assertEquals("-", frequencia.getStringCellValue());
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
        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        avaliacao.setId(id);
        avaliacao.setTema("Avaliacao " + id);
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
        FrequenciaEntity frequencia = new FrequenciaEntity();
        frequencia.setId(1L);
        frequencia.setAluno(aluno);
        frequencia.setPeriodo(periodo);
        frequencia.setPeriodosFaltados(periodosFaltados);
        frequencia.setAtivo(true);
        frequencia.setDataFalta(LocalDateTime.of(2026, 5, 10, 8, 0));
        return frequencia;
    }
}
