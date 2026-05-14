package com.noctua.backend.repository.turma;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Frequencia.FrequenciaEntity;

@Repository
public interface FrequenciaRepository extends JpaRepository<FrequenciaEntity, Long> {

    List<FrequenciaEntity> findByAlunoIdAndAtivoTrue(Long alunoId);

    List<FrequenciaEntity> findByAluno_TurmaIdAndAtivoTrue(Long turmaId);

    List<FrequenciaEntity> findByAluno_TurmaIdAndPeriodoAndAtivoTrue(Long turmaId, Integer periodo);

    List<FrequenciaEntity> findByAluno_TurmaIdAndDataFaltaAndAtivoTrue(Long turmaId, LocalDate dataFalta);

    List<FrequenciaEntity> findByAluno_TurmaIdAndPeriodoAndDataFaltaAndAtivoTrue(Long turmaId, Integer periodo, LocalDate dataFalta);
}
