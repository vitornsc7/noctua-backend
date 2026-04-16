package com.noctua.backend.repository.turma;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Frequencia.FrequenciaEntity;

@Repository
public interface FrequenciaRepository extends JpaRepository<FrequenciaEntity, Long> {

    List<FrequenciaEntity> findByAlunoId(Long alunoId);

    List<FrequenciaEntity> findByAluno_TurmaId(Long turmaId);
}
