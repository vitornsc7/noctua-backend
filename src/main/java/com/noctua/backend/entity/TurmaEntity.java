package com.noctua.backend.entity;

import java.time.LocalDate;

import com.noctua.backend.enums.Turno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "turmas")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class TurmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private LocalDate anoLetivo;

    @Column(nullable = false)
    private Integer qtdePeriodos;

    @Column(nullable = false)
    private Integer qtdeAulasPrevistasPeriodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Turno turno;

    @Column(nullable = false)
    private String disciplina;

    @Column(nullable = false)
    private double mediaMinima;
}