package com.noctua.backend.service.turma;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR"));
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final String LOGO_RESOURCE = "/static/images/logonoctua.svg";

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
            Sheet sheet = wb.createSheet("Boletim anual");
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
            titleCell.setCellValue("Boletim anual - " + turma.getNome() + (turma.getDisciplina() != null ? " | " + turma.getDisciplina() : ""));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, somatorioFtCol));

            Row groupRow = sheet.createRow(1);
            Cell alunoGroupCell = groupRow.createCell(0);
            alunoGroupCell.setCellValue("Aluno");
            alunoGroupCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));

            for (int p = 1; p <= qtdePeriodos; p++) {
                int mdCol = (p - 1) * 2 + 1;
                String bimLabel = p + "º " + abreviarPeriodo(periodoLabel);
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

    public byte[] exportarBoletimAnualPdf(Long turmaId) {
        return converterPlanilhaParaPdf(exportarBoletimAnual(turmaId));
    }

    public byte[] exportarBoletimPeriodoPdf(Long turmaId, Integer periodo) {
        return converterPlanilhaParaPdf(exportarBoletimPeriodo(turmaId, periodo));
    }

    public String gerarNomeArquivoBoletim(Long turmaId, Integer periodo, String extensao) {
        TurmaEntity turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        String tipo = periodo == null
                ? "anual"
                : periodo + "-" + (turma.getQtdePeriodos() == 3 ? "trimestre" : "bimestre");

        String nomeTurma = sanitizarNomeArquivo(turma.getNome());
        String extensaoLimpa = extensao != null && extensao.startsWith(".") ? extensao.substring(1) : extensao;

        return "boletim-" + tipo + "-" + nomeTurma + "." + extensaoLimpa;
    }

    private String sanitizarNomeArquivo(String value) {
        if (value == null || value.isBlank()) return "sem-nome";
        return value.trim()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", " ")
                .replaceAll("(^[.\\s-]+|[.\\s-]+$)", "");
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

    private String formatarCodigoAvaliacao(int index) {
        return "AV" + (index + 1);
    }

    private String abreviarPeriodo(String periodoLabel) {
        return "Trimestre".equalsIgnoreCase(periodoLabel) ? "TRI" : "BI";
    }

    private void criarLegendaAvaliacoes(
            Sheet sheet,
            int startRow,
            int lastColumn,
            List<AvaliacaoEntity> avaliacoes,
            List<AvaliacaoEntity> avaliacoesAnuais,
            String periodoLabel,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {

        if (avaliacoes.isEmpty()) return;

        int mergeEndColumn = Math.max(1, lastColumn);
        Row titleRow = sheet.createRow(startRow);
        criarCelula(titleRow, 0, "Legenda das avaliações", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, mergeEndColumn));

        Row header = sheet.createRow(startRow + 1);
        criarCelula(header, 0, "Código", headerStyle);
        criarCelula(header, 1, "Avaliação", headerStyle);
        if (mergeEndColumn > 1) {
            sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, 1, mergeEndColumn));
        }

        for (int i = 0; i < avaliacoes.size(); i++) {
            AvaliacaoEntity avaliacao = avaliacoes.get(i);
            Row row = sheet.createRow(startRow + 2 + i);
            criarCelula(row, 0, formatarCodigoAvaliacao(i), dataStyle);
            criarCelula(row, 1, formatarLegendaAvaliacao(avaliacao, avaliacoesAnuais, periodoLabel), dataStyle);
            if (mergeEndColumn > 1) {
                sheet.addMergedRegion(new CellRangeAddress(startRow + 2 + i, startRow + 2 + i, 1, mergeEndColumn));
                for (int col = 2; col <= mergeEndColumn; col++) {
                    criarCelula(row, col, "", dataStyle);
                }
            }
        }
    }

    private String formatarLegendaAvaliacao(
            AvaliacaoEntity avaliacao,
            List<AvaliacaoEntity> avaliacoesAnuais,
            String periodoLabel) {

        if (avaliacoesAnuais == null) {
            return formatarDescricaoAvaliacao(avaliacao);
        }

        return avaliacao.getPeriodo() + "º " + periodoLabel + " - " + formatarDescricaoAvaliacao(avaliacao);
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
        criarCelula(header, 1, periodoLabel + ", data e períodos faltados", headerStyle);
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
        criarCelula(header, 1, periodoLabel + ", data e períodos faltados", headerStyle);
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

    private byte[] converterPlanilhaParaPdf(byte[] planilhaBytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(planilhaBytes));
             PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDImageXObject logo = carregarLogo(document);
            PdfWorkbookWriter writer = new PdfWorkbookWriter(document, logo, ZonedDateTime.now(ZONE_ID));

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                writer.writeSheet(workbook.getSheetAt(i));
            }

            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar PDF", e);
        }
    }

    private PDImageXObject carregarLogo(PDDocument document) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(LOGO_RESOURCE)) {
            if (in == null) return null;
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("(?s)<!DOCTYPE[^>]*>", "");

            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 96f);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 64f);

            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            transcoder.transcode(new TranscoderInput(new StringReader(svg)), new TranscoderOutput(pngOut));
            return PDImageXObject.createFromByteArray(document, pngOut.toByteArray(), "logonoctua.png");
        } catch (TranscoderException e) {
            throw new IOException("Erro ao carregar logo do relatório", e);
        }
    }

    private static class PdfWorkbookWriter {
        private static final float MARGIN = 32f;
        private static final float HEADER_HEIGHT = 72f;
        private static final float FOOTER_HEIGHT = 28f;
        private static final float ROW_HEIGHT = 24f;
        private static final float HEADER_ROW_HEIGHT = 29f;
        private static final float TABLE_FONT_SIZE = 8.4f;
        private static final float TABLE_HEADER_FONT_SIZE = 8.8f;

        private final PDDocument document;
        private final PDImageXObject logo;
        private final ZonedDateTime generatedAt;
        private final DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        private PDPageContentStream content;
        private String sectionTitle = "";
        private float y;
        private int pageNumber = 0;
        private PDRectangle pageSize = PDRectangle.A4;

        PdfWorkbookWriter(PDDocument document, PDImageXObject logo, ZonedDateTime generatedAt) {
            this.document = document;
            this.logo = logo;
            this.generatedAt = generatedAt;
        }

        void writeSheet(Sheet sheet) throws IOException {
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();
            int lastCell = findLastCell(sheet);
            if (lastCell <= 0) return;

            Row titleRow = sheet.getRow(firstRow);
            String titleFromSheet = titleRow != null ? formatter.formatCellValue(titleRow.getCell(0)) : "";
            sectionTitle = titleFromSheet == null || titleFromSheet.isBlank() ? sheet.getSheetName() : titleFromSheet;
            PDRectangle sheetPageSize = resolvePageSize(lastCell);

            float maxTableWidth = sheetPageSize.getWidth() - (MARGIN * 2);
            float[] widths = calculateWidths(sheet, firstRow + 1, lastRow, lastCell);
            List<ColumnSlice> columnSlices = buildColumnSlices(widths, maxTableWidth);
            float tableX = MARGIN;

            for (ColumnSlice columnSlice : columnSlices) {
                addPage(sheetPageSize);
                writeRows(sheet, firstRow, lastRow, lastCell, tableX, columnSlice);
            }
        }

        private void writeRows(
                Sheet sheet,
                int firstRow,
                int lastRow,
                int lastCell,
                float tableX,
                ColumnSlice columnSlice) throws IOException {

            int visualRowIndex = 0;
            int dataGroupIndex = -1;
            int currentStyleIndex = 0;
            int skipMergedLastColumnUntilRow = -1;
            boolean mergeLastColumnGroups = columnSlice.containsColumn(lastCell - 1)
                    && isAbsenceTotalColumn(sheet, firstRow + 1, lastCell);

            for (int rowIndex = firstRow + 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlank(row, lastCell)) continue;

                boolean header = visualRowIndex == 0;
                boolean skipLastColumn = !header && mergeLastColumnGroups && rowIndex <= skipMergedLastColumnUntilRow;
                int lastColumnSpanRows = 1;
                if (!header) {
                    String firstCellValue = formatter.formatCellValue(row.getCell(0));
                    if (firstCellValue != null && !firstCellValue.isBlank()) {
                        dataGroupIndex++;
                    }
                    currentStyleIndex = dataGroupIndex < 0 ? visualRowIndex : dataGroupIndex;

                    if (mergeLastColumnGroups && !skipLastColumn) {
                        lastColumnSpanRows = countLastColumnSpanRows(sheet, rowIndex, lastRow, lastCell);
                    }
                }
                float height = header ? HEADER_ROW_HEIGHT : ROW_HEIGHT;
                if (y - (height * lastColumnSpanRows) < MARGIN + FOOTER_HEIGHT) {
                    addPage(pageSize);
                }
                if (lastColumnSpanRows > 1) {
                    skipMergedLastColumnUntilRow = rowIndex + lastColumnSpanRows - 1;
                }
                drawRow(tableX, row, columnSlice, height, header, visualRowIndex, currentStyleIndex, lastColumnSpanRows, skipLastColumn, lastCell);
                y -= height;
                visualRowIndex++;
            }
        }

        void close() throws IOException {
            if (content != null) {
                drawFooter();
                content.close();
            }
        }

        private void addPage(PDRectangle newPageSize) throws IOException {
            if (content != null) {
                drawFooter();
                content.close();
            }

            pageSize = newPageSize;
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            y = pageSize.getHeight() - MARGIN;
            drawDocumentHeader();
            y = pageSize.getHeight() - MARGIN - HEADER_HEIGHT;
        }

        private PDRectangle resolvePageSize(int columnCount) {
            return columnCount > 6
                    ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                    : PDRectangle.A4;
        }

        private int findLastCell(Sheet sheet) {
            int lastCell = 0;
            for (Row row : sheet) {
                if (row != null) {
                    lastCell = Math.max(lastCell, row.getLastCellNum());
                }
            }
            return lastCell;
        }

        private boolean isBlank(Row row, int cellCount) {
            for (int i = 0; i < cellCount; i++) {
                String value = formatter.formatCellValue(row.getCell(i));
                if (value != null && !value.isBlank()) return false;
            }
            return true;
        }

        private boolean isAbsenceTotalColumn(Sheet sheet, int headerRowIndex, int cellCount) {
            Row header = sheet.getRow(headerRowIndex);
            if (header == null || cellCount <= 0) return false;
            String lastHeader = formatter.formatCellValue(header.getCell(cellCount - 1));
            return "Faltas totais".equalsIgnoreCase(lastHeader);
        }

        private int countLastColumnSpanRows(Sheet sheet, int rowIndex, int lastRow, int cellCount) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || cellCount <= 0) return 1;

            String currentTotal = formatter.formatCellValue(row.getCell(cellCount - 1));
            if (currentTotal == null || currentTotal.isBlank()) return 1;

            int span = 1;
            for (int nextRowIndex = rowIndex + 1; nextRowIndex <= lastRow; nextRowIndex++) {
                Row nextRow = sheet.getRow(nextRowIndex);
                if (nextRow == null || isBlank(nextRow, cellCount)) break;

                String nextAluno = formatter.formatCellValue(nextRow.getCell(0));
                String nextTotal = formatter.formatCellValue(nextRow.getCell(cellCount - 1));
                if ((nextAluno != null && !nextAluno.isBlank()) || (nextTotal != null && !nextTotal.isBlank())) {
                    break;
                }
                span++;
            }
            return span;
        }

        private float[] calculateWidths(Sheet sheet, int firstRow, int lastRow, int columnCount) throws IOException {
            float[] widths = new float[columnCount];
            if (columnCount == 0) return widths;

            for (int col = 0; col < columnCount; col++) {
                float maxTextWidth = 0f;
                boolean detailAbsenceColumn = false;
                for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    boolean header = rowIndex == firstRow;
                    String value = sanitizePdfText(formatPdfCellValue(formatter.formatCellValue(row.getCell(col)), header));
                    if (value.toLowerCase(Locale.ROOT).contains("data e períodos faltados")) {
                        detailAbsenceColumn = true;
                    }
                    maxTextWidth = Math.max(maxTextWidth, stringWidth(value, PDType1Font.HELVETICA_BOLD, TABLE_HEADER_FONT_SIZE));
                }

                float minWidth = detailAbsenceColumn ? 215f : (col == 0 ? 105f : 44f);
                float maxWidth = detailAbsenceColumn ? 255f : (col == 0 ? 170f : 135f);
                widths[col] = Math.min(maxWidth, Math.max(minWidth, maxTextWidth + 14f));
            }

            return widths;
        }

        private List<ColumnSlice> buildColumnSlices(float[] widths, float maxTableWidth) {
            if (widths.length == 0) {
                return List.of();
            }

            float totalWidth = sum(widths);
            if (totalWidth <= maxTableWidth) {
                float[] fittedWidths = widths.clone();
                fitWidthsToTable(fittedWidths, maxTableWidth);
                return List.of(new ColumnSlice(allColumns(widths.length), fittedWidths));
            }

            List<ColumnSlice> slices = new ArrayList<>();
            int nextColumn = 1;
            while (nextColumn < widths.length) {
                List<Integer> columns = new ArrayList<>();
                List<Float> sliceWidths = new ArrayList<>();
                columns.add(0);
                sliceWidths.add(widths[0]);
                float currentWidth = widths[0];

                while (nextColumn < widths.length) {
                    float nextWidth = widths[nextColumn];
                    if (columns.size() > 1 && currentWidth + nextWidth > maxTableWidth) {
                        break;
                    }
                    columns.add(nextColumn);
                    sliceWidths.add(nextWidth);
                    currentWidth += nextWidth;
                    nextColumn++;
                    if (currentWidth >= maxTableWidth) {
                        break;
                    }
                }

                float[] fittedWidths = toFloatArray(sliceWidths);
                fitWidthsToTable(fittedWidths, maxTableWidth);
                slices.add(new ColumnSlice(toIntArray(columns), fittedWidths));
            }

            return slices;
        }

        private int[] allColumns(int length) {
            int[] columns = new int[length];
            for (int i = 0; i < length; i++) {
                columns[i] = i;
            }
            return columns;
        }

        private int[] toIntArray(List<Integer> values) {
            int[] result = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }

        private float[] toFloatArray(List<Float> values) {
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }

        private void fitWidthsToTable(float[] widths, float maxTableWidth) {
            float totalWidth = sum(widths);
            if (totalWidth <= maxTableWidth) {
                expandWidthsToTable(widths, maxTableWidth - totalWidth);
                return;
            }

            float[] minWidths = new float[widths.length];
            for (int i = 0; i < widths.length; i++) {
                minWidths[i] = i == 0 ? 92f : 34f;
            }

            float minTotalWidth = sum(minWidths);
            if (minTotalWidth >= maxTableWidth) {
                float scale = maxTableWidth / minTotalWidth;
                for (int i = 0; i < widths.length; i++) {
                    widths[i] = minWidths[i] * scale;
                }
                return;
            }

            float excess = totalWidth - maxTableWidth;
            float shrinkable = totalWidth - minTotalWidth;
            for (int i = 0; i < widths.length; i++) {
                float availableShrink = widths[i] - minWidths[i];
                widths[i] -= excess * (availableShrink / shrinkable);
            }
        }

        private void expandWidthsToTable(float[] widths, float remainingWidth) {
            if (remainingWidth <= 0f || widths.length == 0) {
                return;
            }

            float totalWidth = sum(widths);
            for (int i = 0; i < widths.length; i++) {
                widths[i] += remainingWidth * (widths[i] / totalWidth);
            }
        }

        private float sum(float[] values) {
            float total = 0f;
            for (float value : values) {
                total += value;
            }
            return total;
        }

        private static class ColumnSlice {
            private final int[] columns;
            private final float[] widths;

            private ColumnSlice(int[] columns, float[] widths) {
                this.columns = columns;
                this.widths = widths;
            }

            private boolean containsColumn(int column) {
                for (int value : columns) {
                    if (value == column) {
                        return true;
                    }
                }
                return false;
            }
        }

        private void drawDocumentHeader() throws IOException {
            float rightX = pageSize.getWidth() - MARGIN;
            float brandLeftLimit = logo != null ? rightX - 180f : rightX - 110f;
            float titleFontSize = 12f;
            String fittedTitle = fitText(sectionTitle, PDType1Font.HELVETICA_BOLD, titleFontSize, brandLeftLimit - MARGIN - 12f);
            drawText(fittedTitle, MARGIN, y - 16f, PDType1Font.HELVETICA_BOLD, titleFontSize);

            if (logo != null) {
                content.drawImage(logo, rightX - 24f, y - 22f, 24f, 16f);
            }

            float brandRightX = logo != null ? rightX - 34f : rightX;
            drawTextRight("Noctua", brandRightX, y - 18f, PDType1Font.HELVETICA_BOLD, 16f);

            content.setStrokingColor(55, 81, 102);
            content.setLineWidth(1.2f);
            content.moveTo(MARGIN, y - 46f);
            content.lineTo(pageSize.getWidth() - MARGIN, y - 46f);
            content.stroke();
            content.setLineWidth(1f);
        }
        private void drawRow(
                float startX,
                Row row,
                ColumnSlice columnSlice,
                float height,
                boolean header,
                int visualRowIndex,
                int styleIndex,
                int lastColumnSpanRows,
                boolean skipLastColumn,
                int totalCellCount) throws IOException {
            float x = startX;
            for (int i = 0; i < columnSlice.columns.length; i++) {
                int columnIndex = columnSlice.columns[i];
                String value = formatPdfCellValue(formatter.formatCellValue(row.getCell(columnIndex)), header);
                float width = columnSlice.widths[i];
                boolean originalLastColumn = columnIndex == totalCellCount - 1;
                if (skipLastColumn && originalLastColumn) {
                    break;
                }
                if (header && i + 1 < columnSlice.columns.length && value != null && !value.isBlank()) {
                    int nextColumnIndex = columnSlice.columns[i + 1];
                    String nextValue = formatPdfCellValue(formatter.formatCellValue(row.getCell(nextColumnIndex)), true);
                    if (nextValue == null || nextValue.isBlank()) {
                        width += columnSlice.widths[i + 1];
                        drawCell(x, y, width, height, value, true, visualRowIndex, columnIndex == 0);
                        x += width;
                        i++;
                        continue;
                    }
                }
                float cellHeight = originalLastColumn ? height * lastColumnSpanRows : height;
                drawCell(x, y, width, cellHeight, value, header, styleIndex, columnIndex == 0);
                x += width;
            }
        }

        private void drawCell(float x, float topY, float width, float height, String text, boolean header, int visualRowIndex, boolean firstColumn) throws IOException {
            if (header) {
                if (visualRowIndex == 0) {
                    content.setNonStrokingColor(55, 81, 102);
                } else {
                    content.setNonStrokingColor(81, 121, 153);
                }
                content.addRect(x, topY - height, width, height);
                content.fill();
                content.setNonStrokingColor(255, 255, 255);
            } else {
                if (visualRowIndex % 2 == 0) {
                    content.setNonStrokingColor(255, 255, 255);
                } else {
                    content.setNonStrokingColor(247, 249, 251);
                }
                content.addRect(x, topY - height, width, height);
                content.fill();
                if (firstColumn) {
                    content.setNonStrokingColor(36, 52, 64);
                } else {
                    content.setNonStrokingColor(52, 60, 66);
                }
            }

            content.setStrokingColor(header ? 116 : 221, header ? 142 : 226, header ? 162 : 231);
            content.setLineWidth(header ? 0.6f : 0.35f);
            content.addRect(x, topY - height, width, height);
            content.stroke();
            content.setLineWidth(1f);

            PDType1Font font = header || firstColumn ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
            float fontSize = header ? TABLE_HEADER_FONT_SIZE : TABLE_FONT_SIZE;
            String fitted = fitText(sanitizePdfText(text), font, fontSize, width - 12f);
            float textY = topY - (height / 2f) - (fontSize / 3f);
            if (firstColumn) {
                drawText(fitted, x + 6f, textY, font, fontSize);
            } else {
                drawTextCentered(fitted, x, width, textY, font, fontSize);
            }
        }

        private String fitText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            String value = text == null ? "" : text.replace('\n', ' ');
            if (stringWidth(value, font, fontSize) <= maxWidth) return value;

            String ellipsis = "...";
            while (!value.isEmpty() && stringWidth(value + ellipsis, font, fontSize) > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            return value.isEmpty() ? ellipsis : value + ellipsis;
        }

        private String formatPdfCellValue(String value, boolean header) {
            if (value == null) {
                return value;
            }

            int lineBreak = header ? value.indexOf('\n') : -1;
            String compact = lineBreak >= 0 ? value.substring(0, lineBreak) : value;
            if (header && isPeriodAbsenceSheet() && isAbsenceDetailHeader(compact)) {
                return "Data e períodos faltados";
            }
            return compact
                    .replace("Bimestre", "BI")
                    .replace("bimestre", "BI")
                    .replace("Trimestre", "TRI")
                    .replace("trimestre", "TRI");
        }

        private boolean isPeriodAbsenceSheet() {
            String title = sectionTitle == null ? "" : sectionTitle.toLowerCase(Locale.ROOT);
            return title.startsWith("faltas do ") && !title.startsWith("faltas do ano");
        }

        private boolean isAbsenceDetailHeader(String value) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            return normalized.contains("data e períodos faltados");
        }

        private float stringWidth(String text, PDType1Font font, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000f * fontSize;
        }

        private void drawText(String text, float x, float y, PDType1Font font, float fontSize) throws IOException {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(sanitizePdfText(text));
            content.endText();
        }

        private void drawTextRight(String text, float rightX, float y, PDType1Font font, float fontSize) throws IOException {
            String value = sanitizePdfText(text);
            float width = stringWidth(value, font, fontSize);
            drawText(value, rightX - width, y, font, fontSize);
        }

        private void drawTextCentered(String text, float x, float width, float y, PDType1Font font, float fontSize) throws IOException {
            String value = sanitizePdfText(text);
            float textWidth = stringWidth(value, font, fontSize);
            drawText(value, x + Math.max(4f, (width - textWidth) / 2f), y, font, fontSize);
        }

        private void drawFooter() throws IOException {
            content.setStrokingColor(220, 225, 229);
            content.moveTo(MARGIN, MARGIN + 18f);
            content.lineTo(pageSize.getWidth() - MARGIN, MARGIN + 18f);
            content.stroke();
            drawText("Relatório gerado pelo Noctua em " + DATE_TIME_FORMATTER.format(generatedAt),
                    MARGIN, MARGIN, PDType1Font.HELVETICA, 8f);
            drawTextRight("Página " + pageNumber, pageSize.getWidth() - MARGIN, MARGIN, PDType1Font.HELVETICA, 8f);
        }

        private String sanitizePdfText(String text) {
            return text == null ? "" : text
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\u2018', '\'')
                    .replace('\u2019', '\'')
                    .replace('\u201c', '"')
                    .replace('\u201d', '"');
        }
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
