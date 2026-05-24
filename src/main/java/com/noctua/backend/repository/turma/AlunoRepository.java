package com.noctua.backend.repository.turma;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Aluno.AlunoEntity;

@Repository
public interface AlunoRepository extends JpaRepository<AlunoEntity, Long> {

    List<AlunoEntity> findByTurmaId(Long turmaId);

    List<AlunoEntity> findByTurmaIdAndAtivo(Long turmaId, Boolean ativo);

    Page<AlunoEntity> findByTurmaId(Long turmaId, Pageable pageable);

    Page<AlunoEntity> findByTurmaIdAndAtivo(Long turmaId, Boolean ativo, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AlunoEntity a WHERE a.turma.professor.id = :professorId AND a.ativo = true")
    long countAtivosByProfessorId(@Param("professorId") Long professorId);
}
