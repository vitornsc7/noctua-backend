package com.noctua.backend.dto.Turma;

import java.time.LocalDate;
import java.util.List;

import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.enums.Turno;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class TurmaResponseDTO {
    private Long id;
    private String nome;
    private LocalDate anoLetivo;
    private Integer qtdePeriodos;
    private Integer qtdeAulasPrevistasPeriodo;
    private Turno turno;
    private String disciplina;
    private Double mediaMinima;
    private List<AlunoResponseDTO> alunos;
    private Integer alunosCount;
    private String instituicao;
}
