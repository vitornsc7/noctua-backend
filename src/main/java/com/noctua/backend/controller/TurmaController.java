package com.noctua.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.dto.Turma.TurmaFiltrosDTO;
import com.noctua.backend.service.TurmaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/turmas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaService turmaService;

    @PostMapping
    public ResponseEntity<TurmaResponseDTO> criar(@RequestBody TurmaRequestDTO request) {
        TurmaResponseDTO response = turmaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/filtros")
    public ResponseEntity<TurmaFiltrosDTO> buscarFiltros() {
        return ResponseEntity.ok(turmaService.buscarFiltros());
    }

    @GetMapping
    public ResponseEntity<Page<TurmaResponseDTO>> listar(
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String anoLetivo,
            @RequestParam(required = false) String instituicao) {
        return ResponseEntity.ok(turmaService.listar(pageable, turno, anoLetivo, instituicao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurmaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turmaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurmaResponseDTO> atualizar(@PathVariable Long id, @RequestBody TurmaRequestDTO request) {
        return ResponseEntity.ok(turmaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        turmaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
