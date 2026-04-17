package com.noctua.backend.entity.Usuario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean tokenSenhaUtilizado = false;

    @Column
    private String tokenSenhaReset;

    @Column
    private LocalDateTime tokenSenhaExpiracao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(length = 255)
    private String twoFactorSecret;

    @Column(length = 255)
    private String twoFactorTempSecret;

    @Column
    private LocalDateTime twoFactorConfirmedAt;

}
