package com.noctua.backend.dto.Frequencia;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class FrequenciaResponseDTO {
    private Long id;
    private LocalDateTime dataFalta;
    private Integer periodo;
    private Long alunoId;
}
