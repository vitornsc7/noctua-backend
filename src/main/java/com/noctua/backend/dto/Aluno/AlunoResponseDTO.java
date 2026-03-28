package com.noctua.backend.dto.Aluno;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class AlunoResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo;
    private Long turmaId;
}
