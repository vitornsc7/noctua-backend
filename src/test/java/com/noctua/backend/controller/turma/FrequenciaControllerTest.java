package com.noctua.backend.controller.turma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.noctua.backend.dto.Frequencia.FrequenciaRequestDTO;
import com.noctua.backend.dto.Frequencia.FrequenciaResponseDTO;
import com.noctua.backend.service.turma.FrequenciaService;

@ExtendWith(MockitoExtension.class)
class FrequenciaControllerTest {

    @Mock
    private FrequenciaService frequenciaService;

    @InjectMocks
    private FrequenciaController frequenciaController;

    // Teste 1: endpoint POST /frequencias registra falta e retorna 200.
    @Test
    void registrarFaltaDeveRetornarFrequenciaRegistrada() {
        FrequenciaRequestDTO request = criarRequest();
        FrequenciaResponseDTO serviceResponse = criarResponse();

        when(frequenciaService.registrarFalta(request)).thenReturn(serviceResponse);

        ResponseEntity<FrequenciaResponseDTO> response = frequenciaController.registrarFalta(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(frequenciaService).registrarFalta(request);
    }

    // Teste 2: endpoint GET /frequencias/aluno/{alunoId} lista faltas do aluno.
    @Test
    void listarPorAlunoDeveRetornarFrequenciasDoAluno() {
        List<FrequenciaResponseDTO> serviceResponse = List.of(criarResponse());

        when(frequenciaService.listarPorAluno(100L)).thenReturn(serviceResponse);

        ResponseEntity<List<FrequenciaResponseDTO>> response = frequenciaController.listarPorAluno(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(frequenciaService).listarPorAluno(100L);
    }

    // Teste 3: endpoint GET /frequencias/turma/{turmaId} lista faltas da turma com filtros.
    @Test
    void listarPorTurmaDeveRetornarPaginaDeFrequenciasComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate dataFalta = LocalDate.of(2026, 5, 21);
        Page<FrequenciaResponseDTO> serviceResponse = new PageImpl<>(List.of(criarResponse()), pageable, 1);

        when(frequenciaService.listarPorTurma(10L, 1, dataFalta, 100L, pageable))
                .thenReturn(serviceResponse);

        ResponseEntity<Page<FrequenciaResponseDTO>> response = frequenciaController.listarPorTurma(
                10L,
                1,
                dataFalta,
                100L,
                pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(frequenciaService).listarPorTurma(10L, 1, dataFalta, 100L, pageable);
    }

    // Teste 4: endpoint PUT /frequencias/{id} atualiza registro de falta.
    @Test
    void atualizarFaltaDeveRetornarFrequenciaAtualizada() {
        FrequenciaRequestDTO request = criarRequest();
        FrequenciaResponseDTO serviceResponse = criarResponse();
        serviceResponse.setPeriodosFaltados(3);

        when(frequenciaService.atualizarFalta(50L, request)).thenReturn(serviceResponse);

        ResponseEntity<FrequenciaResponseDTO> response = frequenciaController.atualizarFalta(50L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().getPeriodosFaltados());
        verify(frequenciaService).atualizarFalta(50L, request);
    }

    // Teste 5: endpoint DELETE /frequencias/{id} exclui falta e retorna 204.
    @Test
    void excluirFaltaDeveRetornarNoContent() {
        ResponseEntity<Void> response = frequenciaController.excluirFalta(50L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(frequenciaService).excluirFalta(50L);
    }

    // Teste 6: endpoint GET /frequencias/aluno/{alunoId}/periodo/{periodo}/percentual retorna percentual de frequência.
    @Test
    void calcularPercentualFrequenciaDeveRetornarPercentualDoAlunoNoPeriodo() {
        when(frequenciaService.calcularPercentualFrequencia(100L, 1)).thenReturn(75.0);

        ResponseEntity<Double> response = frequenciaController.calcularPercentualFrequencia(100L, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(75.0, response.getBody());
        verify(frequenciaService).calcularPercentualFrequencia(100L, 1);
    }

    // Teste 7: endpoint GET /frequencias/aluno/{alunoId}/periodo/{periodo}/classificacao retorna classificação da frequência.
    @Test
    void classificarFrequenciaDeveRetornarClassificacaoDoAlunoNoPeriodo() {
        when(frequenciaService.classificarFrequencia(100L, 1)).thenReturn("Atencao");

        ResponseEntity<String> response = frequenciaController.classificarFrequencia(100L, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Atencao", response.getBody());
        verify(frequenciaService).classificarFrequencia(100L, 1);
    }

    private FrequenciaRequestDTO criarRequest() {
        return new FrequenciaRequestDTO(
                LocalDateTime.of(2026, 5, 21, 10, 0),
                1,
                100L,
                2);
    }

    private FrequenciaResponseDTO criarResponse() {
        return new FrequenciaResponseDTO(
                50L,
                LocalDateTime.of(2026, 5, 21, 10, 0),
                1,
                2,
                100L);
    }
}
