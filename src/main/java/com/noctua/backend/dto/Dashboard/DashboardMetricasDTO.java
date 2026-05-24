package com.noctua.backend.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricasDTO {
    private long totalAlunos;
    private long totalTurmas;
    private long totalAvaliacoes;
}
