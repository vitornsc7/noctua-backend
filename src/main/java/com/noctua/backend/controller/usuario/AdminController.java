package com.noctua.backend.controller.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Usuario.AdminRequestDTO;
import com.noctua.backend.service.usuario.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor

public class AdminController {

    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody AdminRequestDTO adminRequestDTO) {
        try {
            adminService.cadastrarAdmin(adminRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Admin cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao cadastrar admin!");
        }
    }
}
