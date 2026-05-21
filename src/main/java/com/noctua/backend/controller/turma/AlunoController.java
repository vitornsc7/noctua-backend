package com.noctua.backend.controller.turma;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.noctua.backend.dto.Aluno.AlunoRequestDTO;
import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.service.GeminiService;
import com.noctua.backend.service.turma.AlunoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/turmas/{turmaId}/alunos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService alunoService;
    private final GeminiService geminiService;

    @PostMapping
    public ResponseEntity<AlunoResponseDTO> criar(@PathVariable Long turmaId, @RequestBody AlunoRequestDTO request) {
        AlunoResponseDTO response = alunoService.criar(turmaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AlunoResponseDTO>> listarPorTurma(
            @PathVariable Long turmaId,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(alunoService.listarPorTurmaPaginado(turmaId, ativo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> buscarPorId(@PathVariable Long turmaId, @PathVariable Long id) {
        return ResponseEntity.ok(alunoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> atualizar(@PathVariable Long turmaId, @PathVariable Long id,
            @RequestBody AlunoRequestDTO request) {
        return ResponseEntity.ok(alunoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long turmaId, @PathVariable Long id) {
        alunoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/importar")
    public ResponseEntity<?> importarComIA(
            Authentication authentication,
            @PathVariable Long turmaId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo vazio."));
        }
        try {
            List<String> nomes = geminiService.extrairNomesAlunos(arquivo, authentication.getName());
            return ResponseEntity.ok(Map.of("nomes", nomes));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("erro", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro ao processar o arquivo: " + e.getMessage()));
        }
    }
}

