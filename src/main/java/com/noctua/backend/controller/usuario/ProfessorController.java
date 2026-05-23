package com.noctua.backend.controller.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Usuario.LimitesRequestDTO;
import com.noctua.backend.dto.Usuario.LimitesResponseDTO;
import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.service.usuario.ProfessorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private static final Logger log = LoggerFactory.getLogger(ProfessorController.class);

    private final ProfessorService professorService;

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody ProfessorRequestDTO professorRequestDTO) {
        try {
            professorService.cadastrarProfessor(professorRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Professor cadastrado com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cadastrar professor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao cadastrar professor: " + e.getMessage());
        }
    }

    @GetMapping("/limites")
    public ResponseEntity<LimitesResponseDTO> getLimites(Authentication authentication) {
        return ResponseEntity.ok(professorService.getLimites(authentication.getName()));
    }

    @PutMapping("/limites")
    public ResponseEntity<LimitesResponseDTO> atualizarLimites(
            Authentication authentication,
            @RequestBody LimitesRequestDTO request) {
        return ResponseEntity.ok(professorService.atualizarLimites(authentication.getName(), request));
    }
}