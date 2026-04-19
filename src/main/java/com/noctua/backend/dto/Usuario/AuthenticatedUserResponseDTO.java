package com.noctua.backend.dto.Usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUserResponseDTO {
    private Long id;
    private String nome;
    private String email;
}