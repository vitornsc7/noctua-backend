package com.noctua.backend.service.turma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void exportarBoletimPeriodoDeveUsarCabecalhoDescritivoEAlinharAlunoAEsquerda() throws Exception {
        TurmaEntity turma = criarTurma();
        AlunoEntity aluno = criarAluno(turma);
        AvaliacaoEntity avaliacao = criarAvaliacao(turma);
        NotaEntity nota = criarNota(aluno, avaliacao);
        FrequenciaEntity primeiraFalta = criarFalta(aluno, 1L, LocalDateTime.of(2026, 4, 15, 8, 0), 2);
        FrequenciaEntity segundaFalta = criarFalta(aluno, 2L, LocalDateTime.of(2026, 4, 16, 8, 0), 1);

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
            assertEquals("Data e períodos faltados", faltasSheet.getRow(1).getCell(1).getStringCellValue());
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
        TurmaEntity turma = criarTurma();
        AlunoEntity aluno = criarAluno(turma);
        AvaliacaoEntity avaliacao = criarAvaliacao(turma);
        NotaEntity nota = criarNota(aluno, avaliacao);
        FrequenciaEntity falta = criarFalta(aluno, 1L, LocalDateTime.of(2026, 4, 15, 8, 0), 2);
        FrequenciaEntity segundaFalta = criarFalta(aluno, 2L, LocalDateTime.of(2026, 8, 20, 8, 0), 3);
        segundaFalta.setPeriodo(2);

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
            assertEquals("Período, data e períodos faltados", faltasSheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("1º Bimestre - 15/04/2026 - 2 faltas", faltasSheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("2º Bimestre - 20/08/2026 - 3 faltas", faltasSheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals(5, (int) faltasSheet.getRow(2).getCell(2).getNumericCellValue());
        }
    }

    private TurmaEntity criarTurma() {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(10L);
        turma.setNome("Turma A");
        turma.setAnoLetivo(LocalDate.of(2026, 1, 1));
        turma.setQtdePeriodos(4);
        turma.setQtdeAulasPrevistasPeriodo(20);
        turma.setTurno(Turno.MATUTINO);
        turma.setDisciplina("Matematica");
        turma.setMediaMinima(6.0);
        turma.setAtivo(true);
        return turma;
    }

    private AlunoEntity criarAluno(TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(100L);
        aluno.setNome("Ana Silva");
        aluno.setAtivo(true);
        aluno.setTurma(turma);
        return aluno;
    }

    private AvaliacaoEntity criarAvaliacao(TurmaEntity turma) {
        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        avaliacao.setId(50L);
        avaliacao.setTema("Algebra");
        avaliacao.setData(LocalDateTime.of(2026, 4, 10, 8, 0));
        avaliacao.setPeso(2);
        avaliacao.setTipo(TipoAvaliacao.PROVA);
        avaliacao.setPeriodo(1);
        avaliacao.setTurma(turma);
        avaliacao.setNumeroChamada(1);
        avaliacao.setConcluida(true);
        return avaliacao;
    }

    private NotaEntity criarNota(AlunoEntity aluno, AvaliacaoEntity avaliacao) {
        NotaEntity nota = new NotaEntity();
        nota.setId(1L);
        nota.setAluno(aluno);
        nota.setAvaliacao(avaliacao);
        nota.setValor(new BigDecimal("8.50"));
        nota.setNaoRealizada(false);
        return nota;
    }

    private FrequenciaEntity criarFalta(AlunoEntity aluno, Long id, LocalDateTime dataFalta, Integer periodosFaltados) {
        FrequenciaEntity falta = new FrequenciaEntity();
        falta.setId(id);
        falta.setAluno(aluno);
        falta.setDataFalta(dataFalta);
        falta.setPeriodo(1);
        falta.setPeriodosFaltados(periodosFaltados);
        falta.setAtivo(true);
        return falta;
    }
}
