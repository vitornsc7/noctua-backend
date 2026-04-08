package com.noctua.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.service.ProfessorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UsuarioController {

    private final ProfessorService professorService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody ProfessorRequestDTO professorRequestDTO) {
        professorService.cadastrarProfessor(professorRequestDTO);
        return ResponseEntity.ok("Professor cadastrado com sucesso!");
    }
}
