package com.noctua.backend.entity.Usuario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="professores")
@Getter
@Setter
@NoArgsConstructor

public class ProfessorEntity extends UsuarioEntity {
    
    @Column(nullable=false, unique=true)
    private String cpf;

    @Column
    private LocalDateTime dataExpiracao;
}
