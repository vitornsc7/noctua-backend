package com.noctua.backend.dto.Usuario;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminMonitoringProfessorDTO {
    private Long id;
    private String nome;
    private String email;
    private Boolean ativo;
    private LocalDateTime dataExpiracao;
}