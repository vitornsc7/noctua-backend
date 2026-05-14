package com.noctua.backend.controller.turma;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Frequencia.FrequenciaRequestDTO;
import com.noctua.backend.dto.Frequencia.FrequenciaResponseDTO;
import com.noctua.backend.service.turma.FrequenciaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/frequencias")
@RequiredArgsConstructor
public class FrequenciaController {

    private final FrequenciaService frequenciaService;

    @PostMapping
    public ResponseEntity<FrequenciaResponseDTO> registrarFalta(@Valid @RequestBody FrequenciaRequestDTO request) {
        return ResponseEntity.ok(frequenciaService.registrarFalta(request));
    }

    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<FrequenciaResponseDTO>> listarPorAluno(@PathVariable Long alunoId) {
        return ResponseEntity.ok(frequenciaService.listarPorAluno(alunoId));
    }

    @GetMapping("/turma/{turmaId}")
    public ResponseEntity<Page<FrequenciaResponseDTO>> listarPorTurma(
            @PathVariable Long turmaId,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) LocalDate dataFalta,
            @PageableDefault(size = 10, sort = "dataFalta", direction = Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(frequenciaService.listarPorTurma(turmaId, periodo, dataFalta, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FrequenciaResponseDTO> atualizarFalta(
            @PathVariable Long id,
            @Valid @RequestBody FrequenciaRequestDTO request) {
        return ResponseEntity.ok(frequenciaService.atualizarFalta(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirFalta(@PathVariable Long id) {
        frequenciaService.excluirFalta(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/aluno/{alunoId}/periodo/{periodo}/percentual")
    public ResponseEntity<Double> calcularPercentualFrequencia(
            @PathVariable Long alunoId,
            @PathVariable Integer periodo) {
        return ResponseEntity.ok(frequenciaService.calcularPercentualFrequencia(alunoId, periodo));
    }

    @GetMapping("/aluno/{alunoId}/periodo/{periodo}/classificacao")
    public ResponseEntity<String> classificarFrequencia(
            @PathVariable Long alunoId,
            @PathVariable Integer periodo) {
        return ResponseEntity.ok(frequenciaService.classificarFrequencia(alunoId, periodo));
    }
}