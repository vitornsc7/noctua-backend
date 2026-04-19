package com.noctua.backend.repository.usuario;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noctua.backend.entity.Usuario.ProfessorEntity;

public interface ProfessorRepository extends JpaRepository<ProfessorEntity, Long> {

    Optional<ProfessorEntity> findByUsuarioEmail(String email);

    Optional<ProfessorEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    List<ProfessorEntity> findAllByOrderByUsuarioNomeAsc();

    long countByUsuarioAtivoTrue();
}
