package com.noctua.backend.controller.turma;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.security.core.Authentication;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Avaliacao.MediaPonderadaTurmaDTO;
import com.noctua.backend.dto.Nota.NotaRequestDTO;
import com.noctua.backend.dto.Nota.NotaResponseDTO;
import com.noctua.backend.enums.TipoAvaliacao;
import com.noctua.backend.service.turma.AvaliacaoService;

@ExtendWith(MockitoExtension.class)
class AvaliacaoControllerTest {

    @Mock
    private AvaliacaoService avaliacaoService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AvaliacaoController avaliacaoController;

    // Teste 1: endpoint POST /turmas/{turmaId}/avaliacoes cria avaliação para o professor autenticado.
    @Test
    void criarDeveRetornarCreatedComAvaliacaoCriada() {
        AvaliacaoRequestDTO request = criarRequest();
        AvaliacaoResponseDTO serviceResponse = criarResponse();

        when(authentication.getName()).thenReturn("prof@email.com");
        when(avaliacaoService.criar("prof@email.com", 10L, request)).thenReturn(serviceResponse);

        ResponseEntity<AvaliacaoResponseDTO> response = avaliacaoController.criar(authentication, 10L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).criar("prof@email.com", 10L, request);
    }

    // Teste 2: endpoint GET /turmas/{turmaId}/avaliacoes lista avaliações paginadas com filtros.
    @Test
    void listarDeveRetornarPaginaDeAvaliacoesComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AvaliacaoResponseDTO> serviceResponse = new PageImpl<>(List.of(criarResponse()), pageable, 1);

        when(avaliacaoService.listarPorTurma(10L, 1, TipoAvaliacao.PROVA, false, pageable))
                .thenReturn(serviceResponse);

        ResponseEntity<Page<AvaliacaoResponseDTO>> response = avaliacaoController.listar(
                10L,
                1,
                TipoAvaliacao.PROVA,
                false,
                pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).listarPorTurma(10L, 1, TipoAvaliacao.PROVA, false, pageable);
    }

    // Teste 3: endpoint GET /turmas/{turmaId}/avaliacoes/{avaliacaoId} busca avaliação por id.
    @Test
    void buscarPorIdDeveRetornarAvaliacao() {
        AvaliacaoResponseDTO serviceResponse = criarResponse();

        when(avaliacaoService.buscarPorId(10L, 50L)).thenReturn(serviceResponse);

        ResponseEntity<AvaliacaoResponseDTO> response = avaliacaoController.buscarPorId(10L, 50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).buscarPorId(10L, 50L);
    }

    // Teste 4: endpoint GET /turmas/{turmaId}/avaliacoes/{avaliacaoId}/notas lista notas da avaliação.
    @Test
    void listarNotasDeveRetornarNotasDaAvaliacao() {
        List<NotaResponseDTO> serviceResponse = List.of(criarNotaResponse());

        when(avaliacaoService.listarNotasPorAvaliacao(10L, 50L)).thenReturn(serviceResponse);

        ResponseEntity<List<NotaResponseDTO>> response = avaliacaoController.listarNotas(10L, 50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).listarNotasPorAvaliacao(10L, 50L);
    }

    // Teste 5: endpoint PUT /turmas/{turmaId}/avaliacoes/{avaliacaoId} atualiza avaliação.
    @Test
    void atualizarDeveRetornarAvaliacaoAtualizada() {
        AvaliacaoRequestDTO request = criarRequest();
        AvaliacaoResponseDTO serviceResponse = criarResponse();
        serviceResponse.setTema("Prova Atualizada");

        when(avaliacaoService.atualizar(10L, 50L, request)).thenReturn(serviceResponse);

        ResponseEntity<AvaliacaoResponseDTO> response = avaliacaoController.atualizar(10L, 50L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Prova Atualizada", response.getBody().getTema());
        verify(avaliacaoService).atualizar(10L, 50L, request);
    }

    // Teste 6: endpoint PUT /turmas/{turmaId}/avaliacoes/{avaliacaoId}/notas/{notaId} atualiza nota.
    @Test
    void atualizarNotaDeveRetornarNotaAtualizada() {
        NotaRequestDTO request = criarNotaRequest();
        NotaResponseDTO serviceResponse = criarNotaResponse();

        when(avaliacaoService.atualizarNota(10L, 50L, 5L, request)).thenReturn(serviceResponse);

        ResponseEntity<NotaResponseDTO> response = avaliacaoController.atualizarNota(10L, 50L, 5L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).atualizarNota(10L, 50L, 5L, request);
    }

    // Teste 7: endpoint POST /turmas/{turmaId}/avaliacoes/{avaliacaoId}/chamada cria chamada subsequente.
    @Test
    void criarChamadaDeveRetornarCreatedComAvaliacaoFilha() {
        AvaliacaoResponseDTO serviceResponse = criarResponse();
        serviceResponse.setNumeroChamada(2);
        serviceResponse.setAvaliacaoPaiId(50L);

        when(authentication.getName()).thenReturn("prof@email.com");
        when(avaliacaoService.criarChamada("prof@email.com", 10L, 50L, null)).thenReturn(serviceResponse);

        ResponseEntity<AvaliacaoResponseDTO> response = avaliacaoController.criarChamada(authentication, 10L, 50L, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).criarChamada("prof@email.com", 10L, 50L, null);
    }

    // Teste 8: endpoint GET /turmas/{turmaId}/avaliacoes/media-ponderada retorna médias da turma.
    @Test
    void calcularMediaPonderadaTurmaDeveRetornarResumoDaTurma() {
        MediaPonderadaTurmaDTO serviceResponse = criarMediaTurma();

        when(avaliacaoService.calcularMediaPonderadaTurma(10L)).thenReturn(serviceResponse);

        ResponseEntity<MediaPonderadaTurmaDTO> response = avaliacaoController.calcularMediaPonderadaTurma(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).calcularMediaPonderadaTurma(10L);
    }

    // Teste 9: endpoint GET /turmas/{turmaId}/avaliacoes/media-ponderada/aluno/{alunoId}/periodo/{periodo} retorna média do aluno.
    @Test
    void calcularMediaPonderadaAlunoDeveRetornarMediaDoAlunoNoPeriodo() {
        BigDecimal serviceResponse = new BigDecimal("8.67");

        when(avaliacaoService.calcularMediaPonderada(100L, 1)).thenReturn(serviceResponse);

        ResponseEntity<BigDecimal> response = avaliacaoController.calcularMediaPonderadaAluno(10L, 100L, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(avaliacaoService).calcularMediaPonderada(100L, 1);
    }

    private AvaliacaoRequestDTO criarRequest() {
        return new AvaliacaoRequestDTO(
                "Prova 1",
                LocalDateTime.of(2026, 5, 21, 10, 0),
                2,
                TipoAvaliacao.PROVA,
                1,
                10L,
                List.of(100L));
    }

    private AvaliacaoResponseDTO criarResponse() {
        return new AvaliacaoResponseDTO(
                50L,
                "Prova 1",
                LocalDateTime.of(2026, 5, 21, 10, 0),
                2,
                TipoAvaliacao.PROVA,
                1,
                10L,
                new BigDecimal("8.50"),
                1,
                1,
                null,
                false,
                false,
                null);
    }

    private NotaRequestDTO criarNotaRequest() {
        return new NotaRequestDTO(new BigDecimal("9.50"), false, 50L, 100L);
    }

    private NotaResponseDTO criarNotaResponse() {
        return new NotaResponseDTO(5L, new BigDecimal("9.50"), false, 50L, 100L, "Ana");
    }

    private MediaPonderadaTurmaDTO criarMediaTurma() {
        MediaPonderadaTurmaDTO.MediaAlunoDTO mediaAluno = new MediaPonderadaTurmaDTO.MediaAlunoDTO(
                100L,
                "Ana",
                Map.of(1, new BigDecimal("8.67")));

        MediaPonderadaTurmaDTO.MediaResumoPeriodoDTO resumo = new MediaPonderadaTurmaDTO.MediaResumoPeriodoDTO(
                new BigDecimal("8.00"),
                new BigDecimal("9.00"),
                null);

        return new MediaPonderadaTurmaDTO(
                List.of(mediaAluno),
                Map.of(1, resumo));
    }
}
