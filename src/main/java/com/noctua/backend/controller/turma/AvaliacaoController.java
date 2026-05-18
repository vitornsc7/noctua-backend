package com.noctua.backend.controller.turma;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Avaliacao.MediaPonderadaTurmaDTO;
import com.noctua.backend.dto.Nota.NotaRequestDTO;
import com.noctua.backend.dto.Nota.NotaResponseDTO;
import com.noctua.backend.enums.TipoAvaliacao;
import com.noctua.backend.service.turma.AvaliacaoService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;

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
    public ResponseEntity<Page<AvaliacaoResponseDTO>> listar(
            @PathVariable Long turmaId,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) TipoAvaliacao tipo,
            @RequestParam(required = false) Boolean concluida,
            @PageableDefault(size = 10, sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(avaliacaoService.listarPorTurma(turmaId, periodo, tipo, concluida, pageable));
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

    @PutMapping("/{avaliacaoId}")
    public ResponseEntity<AvaliacaoResponseDTO> atualizar(
            @PathVariable Long turmaId,
            @PathVariable Long avaliacaoId,
            @RequestBody AvaliacaoRequestDTO request) {
        return ResponseEntity.ok(avaliacaoService.atualizar(turmaId, avaliacaoId, request));
    }

    @PutMapping("/{avaliacaoId}/notas/{notaId}")
    public ResponseEntity<NotaResponseDTO> atualizarNota(
            @PathVariable Long turmaId,
            @PathVariable Long avaliacaoId,
            @PathVariable Long notaId,
            @RequestBody NotaRequestDTO request) {
        return ResponseEntity.ok(avaliacaoService.atualizarNota(turmaId, avaliacaoId, notaId, request));
    }

    @PostMapping("/{avaliacaoId}/chamada")
    public ResponseEntity<AvaliacaoResponseDTO> criarChamada(
            Authentication authentication,
            @PathVariable Long turmaId,
            @PathVariable Long avaliacaoId) {
        AvaliacaoResponseDTO response = avaliacaoService.criarChamada(authentication.getName(), turmaId, avaliacaoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media-ponderada")
    public ResponseEntity<MediaPonderadaTurmaDTO> calcularMediaPonderadaTurma(
            @PathVariable Long turmaId) {
        return ResponseEntity.ok(avaliacaoService.calcularMediaPonderadaTurma(turmaId));
    }

    @GetMapping("/media-ponderada/aluno/{alunoId}/periodo/{periodo}")
    public ResponseEntity<BigDecimal> calcularMediaPonderadaAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId,
            @PathVariable Integer periodo) {
        return ResponseEntity.ok(avaliacaoService.calcularMediaPonderada(alunoId, periodo));
    }
}
