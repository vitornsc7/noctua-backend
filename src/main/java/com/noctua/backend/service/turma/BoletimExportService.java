package com.noctua.backend.service.turma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.entity.Nota.NotaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.AvaliacaoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;
import com.noctua.backend.repository.turma.NotaRepository;
import com.noctua.backend.repository.turma.TurmaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoletimExportService {

    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final NotaRepository notaRepository;
    private final FrequenciaRepository frequenciaRepository;

    public byte[] exportarBoletimAnual(Long turmaId) {
        TurmaEntity turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        List<AlunoEntity> alunos = alunoRepository.findByTurmaIdAndAtivo(turmaId, true)
                .stream()
                .sorted(Comparator.comparing(AlunoEntity::getNome))
                .toList();

        List<AvaliacaoEntity> todasAvaliacoes = avaliacaoRepository.findByTurmaId(turmaId);
        List<NotaEntity> todasNotas = notaRepository.findByAvaliacao_TurmaId(turmaId);

        List<AvaliacaoEntity> rootAvaliacoes = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() == null)
                .toList();

        Map<Long, AvaliacaoEntity> filhaPorPaiId = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() != null)
                .collect(Collectors.toMap(a -> a.getAvaliacaoPai().getId(), a -> a, (x, y) -> y));

        Map<String, NotaEntity> notaMap = todasNotas.stream()
                .collect(Collectors.toMap(
                        n -> n.getAluno().getId() + ":" + n.getAvaliacao().getId(),
                        n -> n,
                        (x, y) -> y));

        int qtdePeriodos = turma.getQtdePeriodos();
        String periodoLabel = qtdePeriodos == 3 ? "Trimestre" : "Bimestre";
        int qtdeAulasPeriodo = turma.getQtdeAulasPrevistasPeriodo();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Boletim Anual");
            sheet.setColumnWidth(0, 8000);
            for (int p = 1; p <= qtdePeriodos; p++) {
                sheet.setColumnWidth((p - 1) * 2 + 1, 3200);
                sheet.setColumnWidth((p - 1) * 2 + 2, 2800);
            }
            int somatorioMdCol = qtdePeriodos * 2 + 1;
            int somatorioFtCol = qtdePeriodos * 2 + 2;
            sheet.setColumnWidth(somatorioMdCol, 3200);
            sheet.setColumnWidth(somatorioFtCol, 2800);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle subHeaderStyle = createSubHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Boletim Anual - " + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""));
            titleCell.setCellStyle(titleStyle);

            Row groupRow = sheet.createRow(1);
            Cell alunoGroupCell = groupRow.createCell(0);
            alunoGroupCell.setCellValue("Aluno");
            alunoGroupCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));

            for (int p = 1; p <= qtdePeriodos; p++) {
                int mdCol = (p - 1) * 2 + 1;
                String bimLabel = p + "º " + periodoLabel.substring(0, 3).toUpperCase();
                Cell periodCell = groupRow.createCell(mdCol);
                periodCell.setCellValue(bimLabel);
                periodCell.setCellStyle(headerStyle);
                groupRow.createCell(mdCol + 1).setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, mdCol, mdCol + 1));
            }

            Cell somatorioCell = groupRow.createCell(somatorioMdCol);
            somatorioCell.setCellValue("Somatório");
            somatorioCell.setCellStyle(headerStyle);
            groupRow.createCell(somatorioFtCol).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, somatorioMdCol, somatorioFtCol));

            Row subRow = sheet.createRow(2);
            subRow.createCell(0).setCellStyle(headerStyle);
            for (int p = 1; p <= qtdePeriodos; p++) {
                int mdCol = (p - 1) * 2 + 1;
                Cell mdCell = subRow.createCell(mdCol);
                mdCell.setCellValue("MD");
                mdCell.setCellStyle(subHeaderStyle);
                Cell ftCell = subRow.createCell(mdCol + 1);
                ftCell.setCellValue("FT");
                ftCell.setCellStyle(subHeaderStyle);
            }
            Cell somMdSub = subRow.createCell(somatorioMdCol);
            somMdSub.setCellValue("MD");
            somMdSub.setCellStyle(subHeaderStyle);
            Cell somFtSub = subRow.createCell(somatorioFtCol);
            somFtSub.setCellValue("FT");
            somFtSub.setCellStyle(subHeaderStyle);

            int rowIdx = 3;
            for (AlunoEntity aluno : alunos) {
                Row row = sheet.createRow(rowIdx++);

                Cell nomeCell = row.createCell(0);
                nomeCell.setCellValue(aluno.getNome());
                nomeCell.setCellStyle(dataStyle);

                List<FrequenciaEntity> faltasAluno = frequenciaRepository.findByAlunoIdAndAtivoTrue(aluno.getId());

                BigDecimal somaMedias = BigDecimal.ZERO;
                int periodsComMedia = 0;
                int totalFaltasGeral = 0;

                for (int p = 1; p <= qtdePeriodos; p++) {
                    int mdCol = (p - 1) * 2 + 1;

                    BigDecimal media = calcularMediaAlunoNoPeriodo(aluno.getId(), p, rootAvaliacoes, filhaPorPaiId, notaMap);
                    Cell mdCell = row.createCell(mdCol);
                    if (media != null) {
                        mdCell.setCellValue(media.doubleValue());
                        somaMedias = somaMedias.add(media);
                        periodsComMedia++;
                    } else {
                        mdCell.setCellValue("--");
                    }
                    mdCell.setCellStyle(dataStyle);

                    final int periodo = p;
                    int faltas = faltasAluno.stream()
                            .filter(f -> f.getPeriodo().equals(periodo))
                            .mapToInt(f -> f.getPeriodosFaltados() != null ? f.getPeriodosFaltados() : 1)
                            .sum();
                    totalFaltasGeral += faltas;

                    Cell ftCell = row.createCell(mdCol + 1);
                    ftCell.setCellValue(faltas);
                    ftCell.setCellStyle(dataStyle);
                }

                Cell somMdCell = row.createCell(somatorioMdCol);
                if (periodsComMedia > 0) {
                    BigDecimal mediaFinal = somaMedias.divide(BigDecimal.valueOf(periodsComMedia), 2, RoundingMode.HALF_UP);
                    somMdCell.setCellValue(mediaFinal.doubleValue());
                } else {
                    somMdCell.setCellValue("--");
                }
                somMdCell.setCellStyle(dataStyle);

                Cell somFtCell = row.createCell(somatorioFtCol);
                somFtCell.setCellValue(totalFaltasGeral);
                somFtCell.setCellStyle(dataStyle);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar exportação");
        }
    }

    public byte[] exportarBoletimPeriodo(Long turmaId, Integer periodo) {
        TurmaEntity turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        List<AlunoEntity> alunos = alunoRepository.findByTurmaIdAndAtivo(turmaId, true)
                .stream()
                .sorted(Comparator.comparing(AlunoEntity::getNome))
                .toList();

        List<AvaliacaoEntity> todasAvaliacoes = avaliacaoRepository.findByTurmaId(turmaId);
        List<NotaEntity> todasNotas = notaRepository.findByAvaliacao_TurmaId(turmaId);

        List<AvaliacaoEntity> rootNoPeriodo = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() == null && a.getPeriodo().equals(periodo))
                .sorted(Comparator.comparing(a -> a.getData() != null ? a.getData() : java.time.LocalDateTime.MIN))
                .toList();

        Map<Long, AvaliacaoEntity> filhaPorPaiId = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() != null)
                .collect(Collectors.toMap(a -> a.getAvaliacaoPai().getId(), a -> a, (x, y) -> y));

        Map<String, NotaEntity> notaMap = todasNotas.stream()
                .collect(Collectors.toMap(
                        n -> n.getAluno().getId() + ":" + n.getAvaliacao().getId(),
                        n -> n,
                        (x, y) -> y));

        int qtdePeriodos = turma.getQtdePeriodos();
        String periodoLabel = qtdePeriodos == 3 ? "Trimestre" : "Bimestre";
        int qtdeAulasPeriodo = turma.getQtdeAulasPrevistasPeriodo();

        try (Workbook wb = new XSSFWorkbook()) {
            String sheetName = periodo + "º " + periodoLabel;
            Sheet sheet = wb.createSheet(sheetName);
            sheet.setColumnWidth(0, 8000);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Boletim " + sheetName + " - " + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""));
            titleCell.setCellStyle(titleStyle);

            Row headerRow = sheet.createRow(1);
            Cell nomeHeader = headerRow.createCell(0);
            nomeHeader.setCellValue("Aluno");
            nomeHeader.setCellStyle(headerStyle);

            for (int i = 0; i < rootNoPeriodo.size(); i++) {
                AvaliacaoEntity av = rootNoPeriodo.get(i);
                Cell cell = headerRow.createCell(i + 1);
                String label = "AV" + (i + 1);
                if (av.getPeso() != null && av.getPeso() != 1) label += " (P" + av.getPeso() + ")";
                cell.setCellValue(label);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i + 1, 3500);
            }

            int mediaCol = rootNoPeriodo.size() + 1;
            int freqCol = rootNoPeriodo.size() + 2;

            Cell mediaHeader = headerRow.createCell(mediaCol);
            mediaHeader.setCellValue("Média");
            mediaHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(mediaCol, 3500);

            Cell freqHeader = headerRow.createCell(freqCol);
            freqHeader.setCellValue("Frequência");
            freqHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(freqCol, 4000);

            int rowIdx = 2;
            for (AlunoEntity aluno : alunos) {
                Row row = sheet.createRow(rowIdx++);

                Cell nomeCell = row.createCell(0);
                nomeCell.setCellValue(aluno.getNome());
                nomeCell.setCellStyle(dataStyle);

                for (int i = 0; i < rootNoPeriodo.size(); i++) {
                    AvaliacaoEntity av = rootNoPeriodo.get(i);
                    NotaEntity nota = notaMap.get(aluno.getId() + ":" + av.getId());

                    Cell cell = row.createCell(i + 1);
                    if (nota == null) {
                        cell.setCellValue("-");
                    } else if (Boolean.TRUE.equals(nota.getNaoRealizada())) {
                        AvaliacaoEntity filha = filhaPorPaiId.get(av.getId());
                        if (filha != null) {
                            NotaEntity notaFilha = notaMap.get(aluno.getId() + ":" + filha.getId());
                            if (notaFilha != null && notaFilha.getValor() != null) {
                                cell.setCellValue(notaFilha.getValor().doubleValue());
                            } else {
                                cell.setCellValue(nota.getValor() != null ? nota.getValor().doubleValue() : 0.0);
                            }
                        } else {
                            cell.setCellValue(nota.getValor() != null ? nota.getValor().doubleValue() : 0.0);
                        }
                    } else if (nota.getValor() != null) {
                        cell.setCellValue(nota.getValor().doubleValue());
                    } else {
                        cell.setCellValue("-");
                    }
                    cell.setCellStyle(dataStyle);
                }

                BigDecimal media = calcularMediaAlunoNoPeriodo(aluno.getId(), periodo, rootNoPeriodo, filhaPorPaiId, notaMap);
                Cell mediaCell = row.createCell(mediaCol);
                mediaCell.setCellValue(media != null ? media.doubleValue() : 0.0);
                mediaCell.setCellStyle(dataStyle);

                List<FrequenciaEntity> faltasAluno = frequenciaRepository.findByAlunoIdAndAtivoTrue(aluno.getId());
                int totalFaltas = faltasAluno.stream()
                        .filter(f -> f.getPeriodo().equals(periodo))
                        .mapToInt(f -> f.getPeriodosFaltados() != null ? f.getPeriodosFaltados() : 1)
                        .sum();
                Cell freqCell = row.createCell(freqCol);
                if (qtdeAulasPeriodo > 0) {
                    double freq = Math.max(0, ((double) (qtdeAulasPeriodo - totalFaltas) / qtdeAulasPeriodo) * 100);
                    freqCell.setCellValue(String.format("%.1f%%", freq).replace('.', ','));
                } else {
                    freqCell.setCellValue("-");
                }
                freqCell.setCellStyle(dataStyle);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar exportação");
        }
    }

    private BigDecimal calcularMediaAlunoNoPeriodo(
            Long alunoId,
            Integer periodo,
            List<AvaliacaoEntity> rootAvaliacoes,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap) {

        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesos = BigDecimal.ZERO;

        for (AvaliacaoEntity avaliacao : rootAvaliacoes) {
            if (!avaliacao.getPeriodo().equals(periodo)) continue;

            BigDecimal notaEfetiva = resolverNotaEfetiva(alunoId, avaliacao, filhaPorPaiId, notaMap);
            if (notaEfetiva != null) {
                BigDecimal peso = BigDecimal.valueOf(avaliacao.getPeso());
                somaPonderada = somaPonderada.add(notaEfetiva.multiply(peso));
                somaPesos = somaPesos.add(peso);
            }
        }

        return somaPesos.compareTo(BigDecimal.ZERO) > 0
                ? somaPonderada.divide(somaPesos, 2, RoundingMode.HALF_UP)
                : null;
    }

    private BigDecimal resolverNotaEfetiva(
            Long alunoId,
            AvaliacaoEntity avaliacao,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap) {

        NotaEntity nota = notaMap.get(alunoId + ":" + avaliacao.getId());
        if (nota == null) return null;

        if (Boolean.TRUE.equals(nota.getNaoRealizada())) {
            AvaliacaoEntity filha = filhaPorPaiId.get(avaliacao.getId());
            if (filha != null) {
                NotaEntity notaFilha = notaMap.get(alunoId + ":" + filha.getId());
                if (notaFilha != null && Boolean.FALSE.equals(notaFilha.getNaoRealizada()) && notaFilha.getValor() != null) {
                    return notaFilha.getValor();
                }
            }
            return nota.getValor() != null ? nota.getValor() : BigDecimal.ZERO;
        }

        if (nota.getValor() != null) return nota.getValor();

        AvaliacaoEntity filha = filhaPorPaiId.get(avaliacao.getId());
        if (filha != null) {
            NotaEntity notaFilha = notaMap.get(alunoId + ":" + filha.getId());
            if (notaFilha != null && Boolean.FALSE.equals(notaFilha.getNaoRealizada()) && notaFilha.getValor() != null) {
                return notaFilha.getValor();
            }
        }

        return null;
    }

    private static final byte[] COLOR_PRIMARY   = { (byte) 0x37, (byte) 0x51, (byte) 0x66 };
    private static final byte[] COLOR_SECONDARY = { (byte) 0x51, (byte) 0x79, (byte) 0x99 };
    private static final byte[] COLOR_WHITE     = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    private CellStyle createSubHeaderStyle(Workbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_WHITE, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_SECONDARY, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(COLOR_WHITE, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_PRIMARY, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        return style;
    }
}
