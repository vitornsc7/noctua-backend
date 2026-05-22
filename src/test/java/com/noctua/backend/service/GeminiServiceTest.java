package com.noctua.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noctua.backend.entity.AiRequestLog.AiRequestLogEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.AiRequestLogRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private MultipartFile arquivo;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(new ObjectMapper(), aiRequestLogRepository, professorRepository);
        ReflectionTestUtils.setField(geminiService, "restClient", restClient);
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-api-key");
    }

    // Teste 1: bloqueia a chamada quando a chave da API não estiver configurada.
    @Test
    void extrairNomesAlunosDeveLancarErroQuandoApiKeyNaoEstiverConfigurada() throws IOException {
        ReflectionTestUtils.setField(geminiService, "apiKey", " ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> geminiService.extrairNomesAlunos(arquivo, "prof@email.com"));

        assertEquals("Chave de API do Gemini não configurada (GOOGLE_API_KEY).", exception.getMessage());
        verify(restClient, never()).post();
    }

    // Teste 2: envia arquivo ao Gemini, extrai nomes e salva log quando tem tokens na resposta.
    @Test
    void extrairNomesAlunosDeveRetornarNomesESalvarLogQuandoRespostaForValida() throws IOException {
        ProfessorEntity professor = criarProfessor(1L);
        String responseJson = criarResponseJson("""
                {
                  "students": [
                    { "name": "Ana Silva" },
                    { "name": "Bruno Souza" },
                    { "name": "   " }
                  ]
                }
                """, 42);

        prepararArquivo("image/png");
        prepararRestClient(responseJson);
        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));

        List<String> nomes = geminiService.extrairNomesAlunos(arquivo, "prof@email.com");

        assertEquals(List.of("Ana Silva", "Bruno Souza"), nomes);
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);

        ArgumentCaptor<AiRequestLogEntity> logCaptor = ArgumentCaptor.forClass(AiRequestLogEntity.class);
        verify(aiRequestLogRepository).save(logCaptor.capture());
        assertEquals(professor, logCaptor.getValue().getProfessor());
        assertEquals(42, logCaptor.getValue().getTokensUsados());
    }

    // Teste 3: usa application/octet-stream quando o arquivo não informa content type.
    @Test
    void extrairNomesAlunosDeveUsarMimeTypePadraoQuandoArquivoNaoInformarContentType() throws IOException {
        String responseJson = criarResponseJson("""
                { "students": [] }
                """, 0);

        prepararArquivo(null);
        prepararRestClient(responseJson);

        List<String> nomes = geminiService.extrairNomesAlunos(arquivo, "prof@email.com");

        assertEquals(List.of(), nomes);
        verify(professorRepository, never()).findByUsuarioEmail(anyString());
        verify(aiRequestLogRepository, never()).save(any());
    }

    // Teste 4: retorna lista vazia quando Gemini não envia candidatos.
    @Test
    void extrairNomesAlunosDeveRetornarListaVaziaQuandoRespostaNaoTiverCandidatos() throws IOException {
        String responseJson = """
                {
                  "candidates": [],
                  "usageMetadata": { "totalTokenCount": 10 }
                }
                """;

        prepararArquivo("image/png");
        prepararRestClient(responseJson);
        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(criarProfessor(1L)));

        List<String> nomes = geminiService.extrairNomesAlunos(arquivo, "prof@email.com");

        assertEquals(List.of(), nomes);
        verify(aiRequestLogRepository).save(any(AiRequestLogEntity.class));
    }

    // Teste 5: retorna lista vazia quando o texto do candidato vem em branco.
    @Test
    void extrairNomesAlunosDeveRetornarListaVaziaQuandoTextoDoCandidatoVierEmBranco() throws IOException {
        String responseJson = criarResponseJson("", 0);

        prepararArquivo("image/png");
        prepararRestClient(responseJson);

        List<String> nomes = geminiService.extrairNomesAlunos(arquivo, "prof@email.com");

        assertEquals(List.of(), nomes);
        verify(aiRequestLogRepository, never()).save(any());
    }

    // Teste 6: propaga erro de parse quando o texto do Gemini não é um JSON válido.
    @Test
    void extrairNomesAlunosDevePropagarIOExceptionQuandoTextoNaoForJsonValido() throws IOException {
        String responseJson = criarResponseJson("nao e json", 0);

        prepararArquivo("image/png");
        prepararRestClient(responseJson);

        assertThrows(IOException.class, () -> geminiService.extrairNomesAlunos(arquivo, "prof@email.com"));
        verify(aiRequestLogRepository, never()).save(any());
    }

    private void prepararArquivo(String contentType) throws IOException {
        when(arquivo.getBytes()).thenReturn("conteudo".getBytes(StandardCharsets.UTF_8));
        when(arquivo.getContentType()).thenReturn(contentType);
    }

    private void prepararRestClient(String responseJson) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(contains("key=test-api-key"))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(responseJson);
    }

    private String criarResponseJson(String textJson, int totalTokens) {
        try {
            return """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": %s
                              }
                            ]
                          }
                        }
                      ],
                      "usageMetadata": {
                        "totalTokenCount": %d
                      }
                    }
                    """.formatted(new ObjectMapper().writeValueAsString(textJson), totalTokens);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ProfessorEntity criarProfessor(Long id) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(id)
                .nome("Professor")
                .email("prof@email.com")
                .senhaHash("hash")
                .ativo(true)
                .build();

        return ProfessorEntity.builder()
                .id(id)
                .usuario(usuario)
                .build();
    }
}
