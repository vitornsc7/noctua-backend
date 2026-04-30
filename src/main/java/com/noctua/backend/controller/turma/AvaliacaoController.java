package com.noctua.backend.controller.turma;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Nota.NotaResponseDTO;
import com.noctua.backend.service.turma.AvaliacaoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/turmas/{turmaId}/avaliacoes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<AvaliacaoResponseDTO> criar(
            Authentication authentication,
            @PathVariable Long turmaId,
            @RequestBody AvaliacaoRequestDTO request) {
        AvaliacaoResponseDTO response = avaliacaoService.criar(authentication.getName(), turmaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AvaliacaoResponseDTO>> listar(
            @PathVariable Long turmaId) {
        return ResponseEntity.ok(avaliacaoService.listarPorTurma(turmaId));
    }

    @GetMapping("/{avaliacaoId}")
    public ResponseEntity<AvaliacaoResponseDTO> buscarPorId(
            @PathVariable Long turmaId,
            @PathVariable Long avaliacaoId) {
        return ResponseEntity.ok(avaliacaoService.buscarPorId(turmaId, avaliacaoId));
    }

    @GetMapping("/{avaliacaoId}/notas")
    public ResponseEntity<List<NotaResponseDTO>> listarNotas(
            @PathVariable Long turmaId,
            @PathVariable Long avaliacaoId) {
        return ResponseEntity.ok(avaliacaoService.listarNotasPorAvaliacao(turmaId, avaliacaoId));
    }
}
