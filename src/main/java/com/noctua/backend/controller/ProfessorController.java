package com.noctua.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.service.ProfessorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService professorService;

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody ProfessorRequestDTO professorRequestDTO) {
        professorService.cadastrarProfessor(professorRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("Professor cadastrado com sucesso.");
    }
}