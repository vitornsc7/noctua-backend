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
import lombok.Setter;

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

    @Column
    private LocalDateTime dataExpiracao;

    @Setter
    @Column(name = "frequencia_atencao_fim")
    private Double frequenciaAtencaoFim;

    @Setter
    @Column(name = "frequencia_regular_fim")
    private Double frequenciaRegularFim;

    @Setter
    @Column(name = "desempenho_acima")
    private Double desempenhoAcima;

    @Setter
    @Column(name = "desempenho_abaixo")
    private Double desempenhoAbaixo;
}
