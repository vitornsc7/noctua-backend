package com.noctua.backend.controller.turma;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.noctua.backend.service.turma.BoletimExportService;

@ExtendWith(MockitoExtension.class)
class BoletimExportControllerTest {

    @Mock
    private BoletimExportService boletimExportService;

    @InjectMocks
    private BoletimExportController boletimExportController;

    @Test
    void exportarBoletimAnualDeveRetornarArquivoExcelComHeaders() {
        byte[] arquivo = new byte[] { 1, 2, 3 };
        when(boletimExportService.exportarBoletimAnual(10L)).thenReturn(arquivo);
        when(boletimExportService.gerarNomeArquivoBoletim(10L, null, "xlsx"))
                .thenReturn("boletim-anual-3º Ano - A.xlsx");

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimAnual(null, 10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                response.getHeaders().getContentType());
        assertEquals("boletim-anual-3º Ano - A.xlsx",
                response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(arquivo, response.getBody());
    }

    @Test
    void exportarBoletimPeriodoDeveRetornarArquivoExcelComHeaders() {
        byte[] arquivo = new byte[] { 4, 5, 6 };
        when(boletimExportService.exportarBoletimPeriodo(10L, 2)).thenReturn(arquivo);
        when(boletimExportService.gerarNomeArquivoBoletim(10L, 2, "xlsx"))
                .thenReturn("boletim-2-bimestre-3º Ano - A.xlsx");

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimPeriodo(null, 10L, 2);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                response.getHeaders().getContentType());
        assertEquals("boletim-2-bimestre-3º Ano - A.xlsx",
                response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(arquivo, response.getBody());
    }

    @Test
    void exportarBoletimAnualPdfDeveRetornarArquivoPdfComHeaders() {
        byte[] arquivo = new byte[] { 7, 8, 9 };
        when(boletimExportService.exportarBoletimAnualPdf(10L)).thenReturn(arquivo);
        when(boletimExportService.gerarNomeArquivoBoletim(10L, null, "pdf"))
                .thenReturn("boletim-anual-3º Ano - A.pdf");

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimAnualPdf(null, 10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("boletim-anual-3º Ano - A.pdf",
                response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(arquivo, response.getBody());
    }

    @Test
    void exportarBoletimPeriodoPdfDeveRetornarArquivoPdfComHeaders() {
        byte[] arquivo = new byte[] { 10, 11, 12 };
        when(boletimExportService.exportarBoletimPeriodoPdf(10L, 2)).thenReturn(arquivo);
        when(boletimExportService.gerarNomeArquivoBoletim(10L, 2, "pdf"))
                .thenReturn("boletim-2-bimestre-3º Ano - A.pdf");

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimPeriodoPdf(null, 10L, 2);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("boletim-2-bimestre-3º Ano - A.pdf",
                response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(arquivo, response.getBody());
    }
}
