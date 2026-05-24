package com.noctua.backend.repository.turma;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;

@Repository
public interface AvaliacaoRepository extends JpaRepository<AvaliacaoEntity, Long>, JpaSpecificationExecutor<AvaliacaoEntity> {

    List<AvaliacaoEntity> findByTurmaId(Long turmaId);
    List<AvaliacaoEntity> findByTurmaIdAndAvaliacaoPaiIsNull(Long turmaId);

    boolean existsByAvaliacaoPaiId(Long avaliacaoPaiId);

    Optional<AvaliacaoEntity> findByAvaliacaoPaiId(Long avaliacaoPaiId);

    @Query("SELECT DISTINCT a FROM AvaliacaoEntity a " +
           "JOIN a.notas n " +
           "WHERE a.turma.professor.id = :professorId " +
           "AND a.data <= :dataLimite " +
           "AND a.concluida = false " +
           "AND n.valor IS NULL")
    List<AvaliacaoEntity> findAvaliacoesPendentesComNotasPorProfessor(
            @Param("professorId") Long professorId,
            @Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT COUNT(a) FROM AvaliacaoEntity a WHERE a.turma.professor.id = :professorId")
    long countByProfessorId(@Param("professorId") Long professorId);
}
