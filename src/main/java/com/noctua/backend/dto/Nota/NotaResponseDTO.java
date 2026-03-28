package com.noctua.backend.dto.Nota;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class NotaResponseDTO {
    private Long id;
    private BigDecimal valor;
    private Boolean naoRealizada;
    private Long avaliacaoId;
    private Long alunoId;
}
