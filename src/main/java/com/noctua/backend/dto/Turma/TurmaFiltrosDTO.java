package com.noctua.backend.dto.Turma;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurmaFiltrosDTO {
    private List<Integer> anos;
    private List<String> instituicoes;
}
