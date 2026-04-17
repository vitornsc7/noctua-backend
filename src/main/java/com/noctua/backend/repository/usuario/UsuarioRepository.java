package com.noctua.backend.repository.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noctua.backend.entity.Usuario.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByTokenSenhaReset(String token);

}