package com.noctua.backend.dto.Usuario;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ProfessorResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private Boolean ativo;
    private String cpf;
    
}
