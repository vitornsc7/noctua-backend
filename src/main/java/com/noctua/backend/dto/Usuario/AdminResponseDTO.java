package com.noctua.backend.dto.Usuario;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class AdminResponseDTO {
    
    private Long id;
    private String nome;
    private String email;
    private Boolean ativo;
}
