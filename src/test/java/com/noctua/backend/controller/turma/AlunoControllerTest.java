package com.noctua.backend.controller.turma;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
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
import org.springframework.web.multipart.MultipartFile;

import com.noctua.backend.dto.Aluno.AlunoRequestDTO;
import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.service.GeminiService;
import com.noctua.backend.service.turma.AlunoService;

@ExtendWith(MockitoExtension.class)
class AlunoControllerTest {

    @Mock
    private AlunoService alunoService;

    @Mock
    private GeminiService geminiService;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile arquivo;

    @InjectMocks
    private AlunoController alunoController;

    // Teste 1: endpoint POST /turmas/{turmaId}/alunos cria aluno e retorna 201.
    @Test
    void criarDeveRetornarCreatedComAlunoCriado() {
        AlunoRequestDTO request = criarRequest();
        AlunoResponseDTO serviceResponse = criarResponse();

        when(alunoService.criar(10L, request)).thenReturn(serviceResponse);

        ResponseEntity<AlunoResponseDTO> response = alunoController.criar(10L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(alunoService).criar(10L, request);
    }

    // Teste 2: endpoint GET /turmas/{turmaId}/alunos lista alunos paginados por turma.
    @Test
    void listarPorTurmaDeveRetornarPaginaDeAlunos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AlunoResponseDTO> serviceResponse = new PageImpl<>(List.of(criarResponse()), pageable, 1);

        when(alunoService.listarPorTurmaPaginado(10L, true, pageable)).thenReturn(serviceResponse);

        ResponseEntity<Page<AlunoResponseDTO>> response = alunoController.listarPorTurma(10L, true, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(alunoService).listarPorTurmaPaginado(10L, true, pageable);
    }

    // Teste 3: endpoint GET /turmas/{turmaId}/alunos/{id} busca aluno pelo id.
    @Test
    void buscarPorIdDeveRetornarAluno() {
        AlunoResponseDTO serviceResponse = criarResponse();

        when(alunoService.buscarPorId(1L)).thenReturn(serviceResponse);

        ResponseEntity<AlunoResponseDTO> response = alunoController.buscarPorId(10L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(alunoService).buscarPorId(1L);
    }

    // Teste 4: endpoint PUT /turmas/{turmaId}/alunos/{id} atualiza aluno.
    @Test
    void atualizarDeveRetornarAlunoAtualizado() {
        AlunoRequestDTO request = criarRequest();
        AlunoResponseDTO serviceResponse = new AlunoResponseDTO(1L, "Ana Maria", "Nova observacao", true, 10L);

        when(alunoService.atualizar(1L, request)).thenReturn(serviceResponse);

        ResponseEntity<AlunoResponseDTO> response = alunoController.atualizar(10L, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(alunoService).atualizar(1L, request);
    }

    // Teste 5: endpoint DELETE /turmas/{turmaId}/alunos/{id} remove aluno e retorna 204.
    @Test
    void deletarDeveRetornarNoContent() {
        ResponseEntity<Void> response = alunoController.deletar(10L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(alunoService).deletar(1L);
    }

    // Teste 6: endpoint POST /turmas/{turmaId}/alunos/importar retorna 400 quando arquivo esta vazio.
    @Test
    void importarComIADeveRetornarBadRequestQuandoArquivoEstiverVazio() throws IOException {
        when(arquivo.isEmpty()).thenReturn(true);

        ResponseEntity<?> response = alunoController.importarComIA(authentication, 10L, arquivo);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Arquivo vazio.", ((Map<?, ?>) response.getBody()).get("erro"));
        verify(geminiService, never()).extrairNomesAlunos(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
    }

    // Teste 7: endpoint de importação retorna nomes extraídos pelo Gemini.
    @Test
    void importarComIADeveRetornarNomesExtraidos() throws IOException {
        when(arquivo.isEmpty()).thenReturn(false);
        when(authentication.getName()).thenReturn("prof@email.com");
        when(geminiService.extrairNomesAlunos(arquivo, "prof@email.com"))
                .thenReturn(List.of("Ana Silva", "Bruno Souza"));

        ResponseEntity<?> response = alunoController.importarComIA(authentication, 10L, arquivo);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("Ana Silva", "Bruno Souza"), ((Map<?, ?>) response.getBody()).get("nomes"));
    }

    // Teste 8: endpoint de importação retorna 503 quando Gemini não está configurado.
    @Test
    void importarComIADeveRetornarServiceUnavailableQuandoGeminiNaoEstiverConfigurado() throws IOException {
        when(arquivo.isEmpty()).thenReturn(false);
        when(authentication.getName()).thenReturn("prof@email.com");
        when(geminiService.extrairNomesAlunos(arquivo, "prof@email.com"))
                .thenThrow(new IllegalStateException("Chave de API não configurada."));

        ResponseEntity<?> response = alunoController.importarComIA(authentication, 10L, arquivo);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Chave de API não configurada.", ((Map<?, ?>) response.getBody()).get("erro"));
    }

    // Teste 9: endpoint de importação retorna 500 quando ocorre erro de leitura do arquivo.
    @Test
    void importarComIADeveRetornarInternalServerErrorQuandoArquivoNaoPuderSerProcessado() throws IOException {
        when(arquivo.isEmpty()).thenReturn(false);
        when(authentication.getName()).thenReturn("prof@email.com");
        when(geminiService.extrairNomesAlunos(arquivo, "prof@email.com"))
                .thenThrow(new IOException("arquivo corrompido"));

        ResponseEntity<?> response = alunoController.importarComIA(authentication, 10L, arquivo);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro ao processar o arquivo: arquivo corrompido", ((Map<?, ?>) response.getBody()).get("erro"));
    }

    private AlunoRequestDTO criarRequest() {
        return new AlunoRequestDTO("Ana", "Observacao", true, 10L);
    }

    private AlunoResponseDTO criarResponse() {
        return new AlunoResponseDTO(1L, "Ana", "Observacao", true, 10L);
    }
}
