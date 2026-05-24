package com.noctua.backend.dto.Avaliacao;

import java.time.LocalDateTime;

import com.noctua.backend.enums.TipoAvaliacao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvisoAvaliacaoPendenteDTO {
    private Long avaliacaoId;
    private Long turmaId;
    private String turmaNome;
    private String tema;
    private TipoAvaliacao tipo;
    private LocalDateTime dataAplicacao;
    private long diasPendentes;
    private int alunosSemNota;
}
