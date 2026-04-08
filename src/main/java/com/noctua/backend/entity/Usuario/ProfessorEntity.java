package com.noctua.backend.entity.Usuario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professores")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ProfessorEntity {

    @Id
    private Long id;

    @OneToOne(optional = false, cascade = jakarta.persistence.CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private UsuarioEntity usuario;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column
    private LocalDateTime dataExpiracao;
}
