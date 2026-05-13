package com.noctua.backend.dto.Frequencia;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class FrequenciaRequestDTO {
    @NotNull(message = "A data da falta é obrigatória.")
    private LocalDateTime dataFalta;

    @NotNull(message = "O período é obrigatório.")
    @Min(value = 1, message = "O período deve ser maior ou igual a 1.")
    private Integer periodo;

    @NotNull(message = "O aluno é obrigatório.")
    private Long alunoId;

    @NotNull(message = "A quantidade de períodos faltados é obrigatória.")
    @Min(value = 1, message = "A quantidade mínima de períodos faltados é 1.")
    @Max(value = 6, message = "A quantidade máxima de períodos faltados é 6.")
    private Integer periodosFaltados;
}
