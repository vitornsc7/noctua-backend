package com.noctua.backend.dto.AiRequestLog;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiRequestLogDTO {
    private Long id;
    private Long professorId;
    private String professorNome;
    private LocalDateTime dataRequest;
    private Integer tokensUsados;
}
