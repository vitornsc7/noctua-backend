package com.noctua.backend.service.turma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import org.apache.poi.ss.usermodel.VerticalAlignment;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                .sorted(Comparator.comparing(AvaliacaoEntity::getPeriodo)
                        .thenComparing(a -> a.getData() != null ? a.getData() : java.time.LocalDateTime.MIN))
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
            CellStyle studentNameStyle = createStudentNameStyle(wb);
            CellStyle stripedDataStyle = createStripedDataStyle(wb);
            CellStyle stripedStudentNameStyle = createStripedStudentNameStyle(wb);
            CellStyle groupNameStyle = createGroupedStudentNameStyle(wb);
            CellStyle stripedGroupNameStyle = createStripedGroupedStudentNameStyle(wb);
            CellStyle mergedDataStyle = createMergedDataStyle(wb);
            CellStyle stripedMergedDataStyle = createStripedMergedDataStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Boletim Anual - " + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, somatorioFtCol));

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
            somatorioCell.setCellValue("Final");
            somatorioCell.setCellStyle(headerStyle);
            groupRow.createCell(somatorioFtCol).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, somatorioMdCol, somatorioFtCol));

            Row subRow = sheet.createRow(2);
            subRow.createCell(0).setCellStyle(headerStyle);
            for (int p = 1; p <= qtdePeriodos; p++) {
                int mdCol = (p - 1) * 2 + 1;
                Cell mdCell = subRow.createCell(mdCol);
                mdCell.setCellValue("Média");
                mdCell.setCellStyle(subHeaderStyle);
                Cell ftCell = subRow.createCell(mdCol + 1);
                ftCell.setCellValue("Faltas");
                ftCell.setCellStyle(subHeaderStyle);
            }
            Cell somMdSub = subRow.createCell(somatorioMdCol);
            somMdSub.setCellValue("Média");
            somMdSub.setCellStyle(subHeaderStyle);
            Cell somFtSub = subRow.createCell(somatorioFtCol);
            somFtSub.setCellValue("Faltas");
            somFtSub.setCellStyle(subHeaderStyle);

            int rowIdx = 3;
            for (int alunoIdx = 0; alunoIdx < alunos.size(); alunoIdx++) {
                AlunoEntity aluno = alunos.get(alunoIdx);
                Row row = sheet.createRow(rowIdx++);
                boolean striped = alunoIdx % 2 == 1;
                CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
                CellStyle rowStudentNameStyle = striped ? stripedStudentNameStyle : studentNameStyle;

                Cell nomeCell = row.createCell(0);
                nomeCell.setCellValue(aluno.getNome());
                nomeCell.setCellStyle(rowStudentNameStyle);

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
                    mdCell.setCellStyle(rowDataStyle);

                    final int periodo = p;
                    int faltas = faltasAluno.stream()
                            .filter(f -> f.getPeriodo().equals(periodo))
                            .mapToInt(f -> f.getPeriodosFaltados() != null ? f.getPeriodosFaltados() : 1)
                            .sum();
                    totalFaltasGeral += faltas;

                    Cell ftCell = row.createCell(mdCol + 1);
                    ftCell.setCellValue(faltas);
                    ftCell.setCellStyle(rowDataStyle);
                }

                Cell somMdCell = row.createCell(somatorioMdCol);
                if (periodsComMedia > 0) {
                    BigDecimal mediaFinal = somaMedias.divide(BigDecimal.valueOf(periodsComMedia), 2, RoundingMode.HALF_UP);
                    somMdCell.setCellValue(mediaFinal.doubleValue());
                } else {
                    somMdCell.setCellValue("--");
                }
                somMdCell.setCellStyle(rowDataStyle);

                Cell somFtCell = row.createCell(somatorioFtCol);
                somFtCell.setCellValue(totalFaltasGeral);
                somFtCell.setCellStyle(rowDataStyle);
            }

            criarAbaDetalhamentoMediasAnual(
                    wb,
                    turma,
                    periodoLabel,
                    alunos,
                    rootAvaliacoes,
                    filhaPorPaiId,
                    notaMap,
                    headerStyle,
                    dataStyle,
                    studentNameStyle,
                    stripedDataStyle,
                    stripedStudentNameStyle,
                    titleStyle);
            criarAbaDetalhamentoFaltasAnual(
                    wb,
                    turma,
                    periodoLabel,
                    alunos,
                    headerStyle,
                    dataStyle,
                    stripedDataStyle,
                    groupNameStyle,
                    stripedGroupNameStyle,
                    mergedDataStyle,
                    stripedMergedDataStyle,
                    titleStyle);

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
            CellStyle studentNameStyle = createStudentNameStyle(wb);
            CellStyle stripedDataStyle = createStripedDataStyle(wb);
            CellStyle stripedStudentNameStyle = createStripedStudentNameStyle(wb);
            CellStyle groupNameStyle = createGroupedStudentNameStyle(wb);
            CellStyle stripedGroupNameStyle = createStripedGroupedStudentNameStyle(wb);
            CellStyle mergedDataStyle = createMergedDataStyle(wb);
            CellStyle stripedMergedDataStyle = createStripedMergedDataStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Boletim " + sheetName + " - " + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(30);
            Cell nomeHeader = headerRow.createCell(0);
            nomeHeader.setCellValue("Aluno");
            nomeHeader.setCellStyle(headerStyle);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 4000);

            int mediaCol = 1;
            int freqCol = 2;

            Cell mediaHeader = headerRow.createCell(mediaCol);
            mediaHeader.setCellValue("Média");
            mediaHeader.setCellStyle(headerStyle);

            Cell freqHeader = headerRow.createCell(freqCol);
            freqHeader.setCellValue("Frequência");
            freqHeader.setCellStyle(headerStyle);

            int rowIdx = 2;
            for (AlunoEntity aluno : alunos) {
                Row row = sheet.createRow(rowIdx++);
                boolean striped = (rowIdx - 3) % 2 == 1;
                CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
                CellStyle rowStudentNameStyle = striped ? stripedStudentNameStyle : studentNameStyle;

                Cell nomeCell = row.createCell(0);
                nomeCell.setCellValue(aluno.getNome());
                nomeCell.setCellStyle(rowStudentNameStyle);

                BigDecimal media = calcularMediaAlunoNoPeriodo(aluno.getId(), periodo, rootNoPeriodo, filhaPorPaiId, notaMap);
                Cell mediaCell = row.createCell(mediaCol);
                mediaCell.setCellValue(media != null ? media.doubleValue() : 0.0);
                mediaCell.setCellStyle(rowDataStyle);

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
                freqCell.setCellStyle(rowDataStyle);
            }

            criarAbaDetalhamentoMediasPeriodo(
                    wb,
                    turma,
                    periodo,
                    periodoLabel,
                    alunos,
                    rootNoPeriodo,
                    filhaPorPaiId,
                    notaMap,
                    headerStyle,
                    dataStyle,
                    studentNameStyle,
                    stripedDataStyle,
                    stripedStudentNameStyle,
                    titleStyle);
            criarAbaDetalhamentoFaltasPeriodo(
                    wb,
                    turma,
                    periodo,
                    periodoLabel,
                    alunos,
                    headerStyle,
                    dataStyle,
                    stripedDataStyle,
                    groupNameStyle,
                    stripedGroupNameStyle,
                    mergedDataStyle,
                    stripedMergedDataStyle,
                    titleStyle);

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

    private String formatarCabecalhoAvaliacao(AvaliacaoEntity avaliacao, int numero) {
        String tema = avaliacao.getTema() != null ? avaliacao.getTema().trim() : "";
        String label = tema.isBlank()
                ? "AV" + numero
                : "AV" + numero + " - " + formatarTipoAvaliacao(avaliacao) + ": " + tema;

        if (avaliacao.getPeso() != null && avaliacao.getPeso() != 1) {
            label += " (P" + avaliacao.getPeso() + ")";
        }

        return label;
    }

    private void ajustarLarguraColunaAoTexto(Sheet sheet, int coluna, String texto, int larguraMinima, int larguraMaxima) {
        int larguraPeloTexto = (texto.length() + 4) * 256;
        int largura = Math.max(larguraMinima, Math.min(larguraPeloTexto, larguraMaxima));
        sheet.setColumnWidth(coluna, largura);
    }

    private void criarAbaDetalhamentoMediasPeriodo(
            Workbook wb,
            TurmaEntity turma,
            Integer periodo,
            String periodoLabel,
            List<AlunoEntity> alunos,
            List<AvaliacaoEntity> avaliacoes,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle studentNameStyle,
            CellStyle stripedDataStyle,
            CellStyle stripedStudentNameStyle,
            CellStyle titleStyle) {

        Sheet sheet = wb.createSheet("Detalhamento de médias");
        sheet.setColumnWidth(0, 8000);
        for (int i = 0; i < avaliacoes.size(); i++) {
            String label = formatarCabecalhoAvaliacao(avaliacoes.get(i), i + 1);
            ajustarLarguraColunaAoTexto(sheet, i + 1, label, 5200, 11000);
        }

        Row titleRow = sheet.createRow(0);
        criarCelula(titleRow, 0, "Avaliações do " + periodo + "º " + periodoLabel.toLowerCase() + " - "
                + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""), titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, avaliacoes.size())));

        Row header = sheet.createRow(1);
        header.setHeightInPoints(30);
        criarCelula(header, 0, "Aluno", headerStyle);
        for (int i = 0; i < avaliacoes.size(); i++) {
            criarCelula(header, i + 1, formatarCabecalhoAvaliacao(avaliacoes.get(i), i + 1), headerStyle);
        }

        int rowIdx = 2;
        for (int alunoIdx = 0; alunoIdx < alunos.size(); alunoIdx++) {
            AlunoEntity aluno = alunos.get(alunoIdx);
            boolean striped = alunoIdx % 2 == 1;
            CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
            CellStyle rowStudentNameStyle = striped ? stripedStudentNameStyle : studentNameStyle;

            Row row = sheet.createRow(rowIdx++);
            criarCelula(row, 0, aluno.getNome(), rowStudentNameStyle);

            for (int i = 0; i < avaliacoes.size(); i++) {
                BigDecimal nota = resolverNotaEfetiva(aluno.getId(), avaliacoes.get(i), filhaPorPaiId, notaMap);
                Cell notaCell = row.createCell(i + 1);
                if (nota != null) {
                    notaCell.setCellValue(nota.doubleValue());
                } else {
                    notaCell.setCellValue("-");
                }
                notaCell.setCellStyle(rowDataStyle);
            }
        }
    }

    private void criarAbaDetalhamentoMediasAnual(
            Workbook wb,
            TurmaEntity turma,
            String periodoLabel,
            List<AlunoEntity> alunos,
            List<AvaliacaoEntity> avaliacoes,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle studentNameStyle,
            CellStyle stripedDataStyle,
            CellStyle stripedStudentNameStyle,
            CellStyle titleStyle) {

        Sheet sheet = wb.createSheet("Detalhamento de médias");
        sheet.setColumnWidth(0, 8000);
        for (int i = 0; i < avaliacoes.size(); i++) {
            String label = formatarCabecalhoAvaliacaoAnual(avaliacoes.get(i), avaliacoes, periodoLabel);
            ajustarLarguraColunaAoTexto(sheet, i + 1, label, 4500, 7200);
        }

        Row titleRow = sheet.createRow(0);
        criarCelula(titleRow, 0, "Avaliações do ano - " + turma.getNome()
                + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""), titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, avaliacoes.size())));

        Row header = sheet.createRow(1);
        header.setHeightInPoints(30);
        criarCelula(header, 0, "Aluno", headerStyle);
        for (int i = 0; i < avaliacoes.size(); i++) {
            criarCelula(header, i + 1, formatarCabecalhoAvaliacaoAnual(avaliacoes.get(i), avaliacoes, periodoLabel), headerStyle);
        }

        int rowIdx = 2;
        for (int alunoIdx = 0; alunoIdx < alunos.size(); alunoIdx++) {
            AlunoEntity aluno = alunos.get(alunoIdx);
            boolean striped = alunoIdx % 2 == 1;
            CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
            CellStyle rowStudentNameStyle = striped ? stripedStudentNameStyle : studentNameStyle;

            Row row = sheet.createRow(rowIdx++);
            criarCelula(row, 0, aluno.getNome(), rowStudentNameStyle);

            for (int i = 0; i < avaliacoes.size(); i++) {
                BigDecimal nota = resolverNotaEfetiva(aluno.getId(), avaliacoes.get(i), filhaPorPaiId, notaMap);
                Cell notaCell = row.createCell(i + 1);
                if (nota != null) {
                    notaCell.setCellValue(nota.doubleValue());
                } else {
                    notaCell.setCellValue("-");
                }
                notaCell.setCellStyle(rowDataStyle);
            }
        }
    }

    private String formatarCabecalhoAvaliacaoAnual(
            AvaliacaoEntity avaliacao,
            List<AvaliacaoEntity> avaliacoes,
            String periodoLabel) {

        int numeroNoPeriodo = 1;
        for (AvaliacaoEntity outraAvaliacao : avaliacoes) {
            if (outraAvaliacao == avaliacao) {
                break;
            }
            if (outraAvaliacao.getPeriodo().equals(avaliacao.getPeriodo())) {
                numeroNoPeriodo++;
            }
        }

        return avaliacao.getPeriodo() + "º " + periodoLabel + " - AV" + numeroNoPeriodo
                + "\n" + formatarDescricaoAvaliacao(avaliacao);
    }

    private String formatarDescricaoAvaliacao(AvaliacaoEntity avaliacao) {
        String tema = avaliacao.getTema() != null ? avaliacao.getTema().trim() : "";
        String descricao = tema.isBlank()
                ? formatarTipoAvaliacao(avaliacao)
                : formatarTipoAvaliacao(avaliacao) + ": " + tema;

        if (avaliacao.getPeso() != null && avaliacao.getPeso() != 1) {
            descricao += " (P" + avaliacao.getPeso() + ")";
        }

        return descricao;
    }

    private void criarAbaDetalhamentoFaltasPeriodo(
            Workbook wb,
            TurmaEntity turma,
            Integer periodo,
            String periodoLabel,
            List<AlunoEntity> alunos,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle stripedDataStyle,
            CellStyle groupNameStyle,
            CellStyle stripedGroupNameStyle,
            CellStyle mergedDataStyle,
            CellStyle stripedMergedDataStyle,
            CellStyle titleStyle) {

        Sheet sheet = wb.createSheet("Detalhamento de faltas");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 7200);
        sheet.setColumnWidth(2, 3600);

        Row titleRow = sheet.createRow(0);
        criarCelula(titleRow, 0, "Faltas do " + periodo + "º " + periodoLabel.toLowerCase() + " - "
                + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""), titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        Row header = sheet.createRow(1);
        header.setHeightInPoints(30);
        criarCelula(header, 0, "Aluno", headerStyle);
        criarCelula(header, 1, "Data e períodos faltados", headerStyle);
        criarCelula(header, 2, "Faltas totais", headerStyle);

        int rowIdx = 2;
        int grupoIdx = 0;
        for (AlunoEntity aluno : alunos) {
            List<FrequenciaEntity> faltasAluno = frequenciaRepository.findByAlunoIdAndAtivoTrue(aluno.getId()).stream()
                    .filter(f -> f.getPeriodo().equals(periodo))
                    .sorted(Comparator.comparing(FrequenciaEntity::getDataFalta))
                    .toList();

            if (faltasAluno.isEmpty()) {
                continue;
            }

            boolean striped = grupoIdx++ % 2 == 1;
            CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
            CellStyle rowGroupNameStyle = striped ? stripedGroupNameStyle : groupNameStyle;
            CellStyle rowMergedDataStyle = striped ? stripedMergedDataStyle : mergedDataStyle;

            int totalFaltasAluno = 0;
            for (int i = 0; i < faltasAluno.size(); i++) {
                FrequenciaEntity falta = faltasAluno.get(i);
                int periodosFaltados = falta.getPeriodosFaltados() != null ? falta.getPeriodosFaltados() : 1;
                totalFaltasAluno += periodosFaltados;

                Row row = sheet.createRow(rowIdx++);
                criarCelula(row, 0, i == 0 ? aluno.getNome() : "", rowGroupNameStyle);
                criarCelula(row, 1, formatarDetalheFalta(falta, false), rowDataStyle);
                criarCelula(row, 2, "", rowMergedDataStyle);
            }

            int firstRow = rowIdx - faltasAluno.size();
            int lastRow = rowIdx - 1;
            sheet.getRow(firstRow).getCell(2).setCellValue(totalFaltasAluno);
            mesclarLinhasDoAluno(sheet, firstRow, lastRow);
        }
    }

    private void criarAbaDetalhamentoFaltasAnual(
            Workbook wb,
            TurmaEntity turma,
            String periodoLabel,
            List<AlunoEntity> alunos,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle stripedDataStyle,
            CellStyle groupNameStyle,
            CellStyle stripedGroupNameStyle,
            CellStyle mergedDataStyle,
            CellStyle stripedMergedDataStyle,
            CellStyle titleStyle) {

        Sheet sheet = wb.createSheet("Detalhamento de faltas");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 3600);

        Row titleRow = sheet.createRow(0);
        criarCelula(titleRow, 0, "Faltas do ano - " + turma.getNome()
                + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""), titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        Row header = sheet.createRow(1);
        header.setHeightInPoints(30);
        criarCelula(header, 0, "Aluno", headerStyle);
        criarCelula(header, 1, "Período, data e períodos faltados", headerStyle);
        criarCelula(header, 2, "Faltas totais", headerStyle);

        int rowIdx = 2;
        int grupoIdx = 0;
        for (AlunoEntity aluno : alunos) {
            List<FrequenciaEntity> faltasAluno = frequenciaRepository.findByAlunoIdAndAtivoTrue(aluno.getId()).stream()
                    .sorted(Comparator.comparing(FrequenciaEntity::getPeriodo)
                            .thenComparing(FrequenciaEntity::getDataFalta))
                    .toList();

            if (faltasAluno.isEmpty()) {
                continue;
            }

            boolean striped = grupoIdx++ % 2 == 1;
            CellStyle rowDataStyle = striped ? stripedDataStyle : dataStyle;
            CellStyle rowGroupNameStyle = striped ? stripedGroupNameStyle : groupNameStyle;
            CellStyle rowMergedDataStyle = striped ? stripedMergedDataStyle : mergedDataStyle;

            int totalFaltasAluno = 0;
            for (int i = 0; i < faltasAluno.size(); i++) {
                FrequenciaEntity falta = faltasAluno.get(i);
                int periodosFaltados = falta.getPeriodosFaltados() != null ? falta.getPeriodosFaltados() : 1;
                totalFaltasAluno += periodosFaltados;

                Row row = sheet.createRow(rowIdx++);
                criarCelula(row, 0, i == 0 ? aluno.getNome() : "", rowGroupNameStyle);
                criarCelula(row, 1, formatarDetalheFaltaAnual(falta, periodoLabel), rowDataStyle);
                criarCelula(row, 2, "", rowMergedDataStyle);
            }

            int firstRow = rowIdx - faltasAluno.size();
            int lastRow = rowIdx - 1;
            sheet.getRow(firstRow).getCell(2).setCellValue(totalFaltasAluno);
            mesclarLinhasDoAluno(sheet, firstRow, lastRow);
        }
    }

    private String formatarDetalheFalta(FrequenciaEntity falta, boolean incluirPeriodo) {
        int periodosFaltados = falta.getPeriodosFaltados() != null ? falta.getPeriodosFaltados() : 1;
        String detalhe = formatarDataFalta(falta) + " - " + periodosFaltados + " "
                + (periodosFaltados == 1 ? "falta" : "faltas");

        return incluirPeriodo ? falta.getPeriodo() + " periodo - " + detalhe : detalhe;
    }

    private String formatarDetalheFaltaAnual(FrequenciaEntity falta, String periodoLabel) {
        int periodosFaltados = falta.getPeriodosFaltados() != null ? falta.getPeriodosFaltados() : 1;
        return falta.getPeriodo() + "º " + periodoLabel + " - " + formatarDataFalta(falta) + " - "
                + periodosFaltados + " " + (periodosFaltados == 1 ? "falta" : "faltas");
    }

    private String formatarDataFalta(FrequenciaEntity falta) {
        return falta.getDataFalta() != null ? falta.getDataFalta().format(DATE_FORMATTER) : "-";
    }

    private void mesclarLinhasDoAluno(Sheet sheet, int firstRow, int lastRow) {
        if (lastRow > firstRow) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, 2, 2));
        }
    }

    private void criarCelula(Row row, int coluna, String valor, CellStyle style) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        cell.setCellStyle(style);
    }

    private void criarCelula(Row row, int coluna, int valor, CellStyle style) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        cell.setCellStyle(style);
    }

    private String formatarTipoAvaliacao(AvaliacaoEntity avaliacao) {
        if (avaliacao.getTipo() == null) {
            return "Avaliacao";
        }

        return switch (avaliacao.getTipo()) {
            case PROVA -> "Prova";
            case TRABALHO -> "Trabalho";
            case ATIVIDADE -> "Atividade";
        };
    }

    private static final byte[] COLOR_PRIMARY   = { (byte) 0x37, (byte) 0x51, (byte) 0x66 };
    private static final byte[] COLOR_SECONDARY = { (byte) 0x51, (byte) 0x79, (byte) 0x99 };
    private static final byte[] COLOR_WHITE     = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    private static final byte[] COLOR_STRIPE    = { (byte) 0xEF, (byte) 0xEF, (byte) 0xEF };

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
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
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

    private CellStyle createStripedDataStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        applyStripeFill(style);
        return style;
    }

    private CellStyle createStudentNameStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createStripedStudentNameStyle(Workbook wb) {
        CellStyle style = createStudentNameStyle(wb);
        applyStripeFill(style);
        return style;
    }

    private CellStyle createGroupedStudentNameStyle(Workbook wb) {
        CellStyle style = createStudentNameStyle(wb);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private CellStyle createStripedGroupedStudentNameStyle(Workbook wb) {
        CellStyle style = createGroupedStudentNameStyle(wb);
        applyStripeFill(style);
        return style;
    }

    private CellStyle createMergedDataStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createStripedMergedDataStyle(Workbook wb) {
        CellStyle style = createMergedDataStyle(wb);
        applyStripeFill(style);
        return style;
    }

    private void applyStripeFill(CellStyle style) {
        XSSFCellStyle xssfStyle = (XSSFCellStyle) style;
        xssfStyle.setFillForegroundColor(new XSSFColor(COLOR_STRIPE, null));
        xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
