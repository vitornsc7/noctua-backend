package com.noctua.backend.dto.Usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitesResponseDTO {
    private Double atencaoFim;
    private Double regularFim;
    private Double pontosAcima;
    private Double pontosAbaixo;
}
