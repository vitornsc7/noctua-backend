package com.noctua.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Turma.TurmaEntity;

@Repository
public interface TurmaRepository extends JpaRepository<TurmaEntity, Long>, JpaSpecificationExecutor<TurmaEntity> {

    @Query("SELECT DISTINCT YEAR(t.anoLetivo) FROM TurmaEntity t ORDER BY 1 DESC")
    List<Integer> findDistinctAnos();

    @Query("SELECT DISTINCT t.instituicao FROM TurmaEntity t WHERE t.instituicao IS NOT NULL AND t.instituicao <> '' ORDER BY t.instituicao ASC")
    List<String> findDistinctInstituicoes();
}
