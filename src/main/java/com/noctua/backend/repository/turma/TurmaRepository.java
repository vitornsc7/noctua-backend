package com.noctua.backend.repository.turma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Turma.TurmaEntity;

@Repository
public interface TurmaRepository extends JpaRepository<TurmaEntity, Long>, JpaSpecificationExecutor<TurmaEntity> {

    Optional<TurmaEntity> findByIdAndProfessorIdAndAtivoTrue(Long id, Long professorId);

    boolean existsByIdAndProfessorIdAndAtivoTrue(Long id, Long professorId);

    @Query("SELECT DISTINCT YEAR(t.anoLetivo) FROM TurmaEntity t WHERE t.professor.id = :professorId AND t.ativo = true ORDER BY 1 DESC")
    List<Integer> findDistinctAnosByProfessorId(@Param("professorId") Long professorId);

    @Query("SELECT DISTINCT t.instituicao FROM TurmaEntity t WHERE t.professor.id = :professorId AND t.ativo = true AND t.instituicao IS NOT NULL AND t.instituicao <> '' ORDER BY t.instituicao ASC")
    List<String> findDistinctInstituicoesByProfessorId(@Param("professorId") Long professorId);
}
