package com.noctua.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noctua.backend.entity.AiRequestLog.AiRequestLogEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.repository.AiRequestLogRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String OCR_PROMPT = """
            You are an OCR and data extraction assistant.

            Your task is to analyze the provided image, scanned document, spreadsheet screenshot, or PDF page and extract ONLY the student names that appear in the document.

            Rules:
            - Return ONLY valid JSON.
            - Do not include explanations, markdown, comments, or extra text.
            - Preserve the original spelling of the names exactly as they appear.
            - Ignore numbers, IDs, grades, emails, headers, table titles, and unrelated text.
            - Remove duplicate names.
            - Keep the same order in which the names appear in the document.
            - If a name is incomplete or uncertain, still include it exactly as visible.
            - If no student names are found, return:
            {
              "students": []
            }

            Expected JSON format:

            {
              "students": [
                {
                  "name": "John Doe"
                },
                {
                  "name": "Jane Smith"
                }
              ]
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final ProfessorRepository professorRepository;

    public GeminiService(ObjectMapper objectMapper, AiRequestLogRepository aiRequestLogRepository, ProfessorRepository professorRepository) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.aiRequestLogRepository = aiRequestLogRepository;
        this.professorRepository = professorRepository;
    }

    public List<String> extrairNomesAlunos(MultipartFile arquivo, String emailProfessor) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chave de API do Gemini não configurada (GOOGLE_API_KEY).");
        }

        String base64Data = Base64.getEncoder().encodeToString(arquivo.getBytes());
        String mimeType = arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Data
                                )),
                                Map.of("text", OCR_PROMPT)
                        )
                )),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        String responseBody = restClient.post()
                .uri(GEMINI_URL + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        List<String> nomes = parseStudentNames(responseBody);
        salvarLog(responseBody, emailProfessor);
        return nomes;
    }

    private void salvarLog(String responseJson, String emailProfessor) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            int totalTokens = root.path("usageMetadata").path("totalTokenCount").asInt(0);
            if (totalTokens == 0) return;
            ProfessorEntity professor = professorRepository.findByUsuarioEmail(emailProfessor)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));
            AiRequestLogEntity log = new AiRequestLogEntity();
            log.setProfessor(professor);
            log.setDataRequest(LocalDateTime.now());
            log.setTokensUsados(totalTokens);
            aiRequestLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Não foi possível salvar log de tokens da IA: {}", e.getMessage());
        }
    }

    private List<String> parseStudentNames(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.warn("Gemini retornou resposta sem candidatos: {}", responseJson);
            return List.of();
        }
        String text = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText("");

        if (text.isBlank()) {
            return List.of();
        }

        JsonNode studentsRoot = objectMapper.readTree(text);
        JsonNode students = studentsRoot.path("students");

        List<String> names = new ArrayList<>();
        if (students.isArray()) {
            for (JsonNode student : students) {
                String name = student.path("name").asText("").trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }
}
