package com.noctua.backend.controller.turma;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimAnual(null, 10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getContentDisposition().toString()
                .contains("boletim-anual-turma-10.xlsx"));
        assertArrayEquals(arquivo, response.getBody());
    }

    @Test
    void exportarBoletimPeriodoDeveRetornarArquivoExcelComHeaders() {
        byte[] arquivo = new byte[] { 4, 5, 6 };
        when(boletimExportService.exportarBoletimPeriodo(10L, 2)).thenReturn(arquivo);

        ResponseEntity<byte[]> response = boletimExportController.exportarBoletimPeriodo(null, 10L, 2);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getContentDisposition().toString()
                .contains("boletim-periodo-2-turma-10.xlsx"));
        assertArrayEquals(arquivo, response.getBody());
    }
}
