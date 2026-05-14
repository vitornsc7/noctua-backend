package com.noctua.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.AiRequestLog.AiRequestLogEntity;

@Repository
public interface AiRequestLogRepository extends JpaRepository<AiRequestLogEntity, Long> {

    List<AiRequestLogEntity> findAllByOrderByDataRequestDesc();

    List<AiRequestLogEntity> findByProfessorIdOrderByDataRequestDesc(Long professorId);

    @Query("SELECT l.professor.id, SUM(l.tokensUsados) FROM AiRequestLogEntity l GROUP BY l.professor.id")
    List<Object[]> sumTokensPorProfessor();
}
