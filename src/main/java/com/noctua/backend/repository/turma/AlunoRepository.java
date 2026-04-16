package com.noctua.backend.repository.turma;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Aluno.AlunoEntity;

@Repository
public interface AlunoRepository extends JpaRepository<AlunoEntity, Long> {

    List<AlunoEntity> findByTurmaId(Long turmaId);
}
