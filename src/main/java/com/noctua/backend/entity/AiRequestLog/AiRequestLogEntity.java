package com.noctua.backend.entity.AiRequestLog;

import java.time.LocalDateTime;

import com.noctua.backend.entity.Usuario.ProfessorEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_request_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private ProfessorEntity professor;

    @Column(nullable = false)
    private LocalDateTime dataRequest;

    @Column(nullable = false)
    private Integer tokensUsados;
}
