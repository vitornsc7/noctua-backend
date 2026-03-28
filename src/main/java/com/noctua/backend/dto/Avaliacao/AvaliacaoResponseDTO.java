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

public class AvaliacaoResponseDTO {
    private Long id;
    private String tema;
    private LocalDateTime data;
    private Integer peso;
    private TipoAvaliacao tipo;
    private Integer periodo;
    private Long turmaId;
}
