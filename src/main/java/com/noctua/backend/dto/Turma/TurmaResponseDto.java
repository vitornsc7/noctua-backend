package com.noctua.backend.dto.Turma;

import java.time.LocalDate;

import com.noctua.backend.enums.Turno;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class TurmaResponseDto {
    private Long id;
    private String nome;
    private LocalDate anoLetivo;
    private Integer qtdePeriodos;
    private Integer qtdeAulasPrevistasPeriodo;
    private Turno turno;
    private String disciplina;
    private Double mediaMinima;
    
}
