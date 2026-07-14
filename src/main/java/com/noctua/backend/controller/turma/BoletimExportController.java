package com.noctua.backend.controller.turma;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.service.turma.BoletimExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/turmas/{turmaId}/boletim")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BoletimExportController {

    private final BoletimExportService boletimExportService;

    @GetMapping("/export/anual")
    public ResponseEntity<byte[]> exportarBoletimAnual(
            Authentication authentication,
            @PathVariable Long turmaId) {

        byte[] bytes = boletimExportService.exportarBoletimAnual(turmaId);

        HttpHeaders headers = criarHeaders(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                boletimExportService.gerarNomeArquivoBoletim(turmaId, null, "xlsx"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/anual/pdf")
    public ResponseEntity<byte[]> exportarBoletimAnualPdf(
            Authentication authentication,
            @PathVariable Long turmaId) {

        byte[] bytes = boletimExportService.exportarBoletimAnualPdf(turmaId);

        HttpHeaders headers = criarHeaders(
                MediaType.APPLICATION_PDF,
                boletimExportService.gerarNomeArquivoBoletim(turmaId, null, "pdf"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/periodo/{periodo}")
    public ResponseEntity<byte[]> exportarBoletimPeriodo(
            Authentication authentication,
            @PathVariable Long turmaId,
            @PathVariable Integer periodo) {

        byte[] bytes = boletimExportService.exportarBoletimPeriodo(turmaId, periodo);

        HttpHeaders headers = criarHeaders(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                boletimExportService.gerarNomeArquivoBoletim(turmaId, periodo, "xlsx"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/periodo/{periodo}/pdf")
    public ResponseEntity<byte[]> exportarBoletimPeriodoPdf(
            Authentication authentication,
            @PathVariable Long turmaId,
            @PathVariable Integer periodo) {

        byte[] bytes = boletimExportService.exportarBoletimPeriodoPdf(turmaId, periodo);

        HttpHeaders headers = criarHeaders(
                MediaType.APPLICATION_PDF,
                boletimExportService.gerarNomeArquivoBoletim(turmaId, periodo, "pdf"));

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private HttpHeaders criarHeaders(MediaType contentType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setAccessControlExposeHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return headers;
    }
}
