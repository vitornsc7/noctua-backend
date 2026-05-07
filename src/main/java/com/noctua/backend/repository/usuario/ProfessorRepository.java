package com.noctua.backend.repository.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noctua.backend.entity.Usuario.ProfessorEntity;

public interface ProfessorRepository extends JpaRepository<ProfessorEntity, Long> {

    Optional<ProfessorEntity> findByUsuarioEmail(String email);

    List<ProfessorEntity> findAllByOrderByUsuarioNomeAsc();

    long countByUsuarioAtivoTrue();
}
