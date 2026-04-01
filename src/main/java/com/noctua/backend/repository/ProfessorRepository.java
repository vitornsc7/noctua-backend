package com.noctua.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noctua.backend.entity.Usuario.ProfessorEntity;

public interface ProfessorRepository extends JpaRepository<ProfessorEntity, Long> {

    Optional<ProfessorEntity> findByEmail(String email);

    Optional<ProfessorEntity> findByCpf(String cpf);
}
