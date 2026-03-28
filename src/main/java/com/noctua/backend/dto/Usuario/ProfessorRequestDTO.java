package com.noctua.backend.dto.Usuario;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ProfessorRequestDTO {

    private String nome;
    private String email;
    private String senha;
    private Boolean ativo;
    private String cpf;
    private LocalDateTime dataExpiracao;
    
}
