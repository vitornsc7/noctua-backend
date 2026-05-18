package com.noctua.backend.dto.Avaliacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaPonderadaTurmaDTO {

    private List<MediaAlunoDTO> mediasAlunos;
    private Map<Integer, MediaResumoPeriodoDTO> resumoPorPeriodo;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaAlunoDTO {
        private Long alunoId;
        private String alunoNome;
        private Map<Integer, BigDecimal> mediaPorPeriodo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResumoPeriodoDTO {
        private BigDecimal mediaProva;
        private BigDecimal mediaTrabalho;
        private BigDecimal mediaAtividade;
    }
}
