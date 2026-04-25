package com.noctua.backend.dto.Usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioUpdateRequestDTO {
    private String nome;
    private String email;
    private String senha;
}