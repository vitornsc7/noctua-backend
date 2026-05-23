package com.noctua.backend.dto.Usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LimitesRequestDTO {
    private Double atencaoFim;
    private Double regularFim;
    private Double pontosAcima;
    private Double pontosAbaixo;
}
