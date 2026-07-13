package com.noctua.backend.controller.turma;

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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(boletimExportService.gerarNomeArquivoBoletim(turmaId, null, "xlsx"))
                .build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/anual/pdf")
    public ResponseEntity<byte[]> exportarBoletimAnualPdf(
            Authentication authentication,
            @PathVariable Long turmaId) {

        byte[] bytes = boletimExportService.exportarBoletimAnualPdf(turmaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(boletimExportService.gerarNomeArquivoBoletim(turmaId, null, "pdf"))
                .build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/periodo/{periodo}")
    public ResponseEntity<byte[]> exportarBoletimPeriodo(
            Authentication authentication,
            @PathVariable Long turmaId,
            @PathVariable Integer periodo) {

        byte[] bytes = boletimExportService.exportarBoletimPeriodo(turmaId, periodo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(boletimExportService.gerarNomeArquivoBoletim(turmaId, periodo, "xlsx"))
                .build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/export/periodo/{periodo}/pdf")
    public ResponseEntity<byte[]> exportarBoletimPeriodoPdf(
            Authentication authentication,
            @PathVariable Long turmaId,
            @PathVariable Integer periodo) {

        byte[] bytes = boletimExportService.exportarBoletimPeriodoPdf(turmaId, periodo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(boletimExportService.gerarNomeArquivoBoletim(turmaId, periodo, "pdf"))
                .build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
