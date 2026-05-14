package com.noctua.backend.repository.turma;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Frequencia.FrequenciaEntity;

@Repository
public interface FrequenciaRepository extends JpaRepository<FrequenciaEntity, Long> {

    List<FrequenciaEntity> findByAlunoIdAndAtivoTrue(Long alunoId);

    Page<FrequenciaEntity> findByAluno_TurmaIdAndAtivoTrue(Long turmaId, Pageable pageable);

    Page<FrequenciaEntity> findByAluno_TurmaIdAndPeriodoAndAtivoTrue(Long turmaId, Integer periodo, Pageable pageable);

    Page<FrequenciaEntity> findByAluno_TurmaIdAndDataFaltaGreaterThanEqualAndDataFaltaLessThanAndAtivoTrue(Long turmaId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<FrequenciaEntity> findByAluno_TurmaIdAndPeriodoAndDataFaltaGreaterThanEqualAndDataFaltaLessThanAndAtivoTrue(Long turmaId, Integer periodo, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
