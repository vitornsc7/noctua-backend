package com.noctua.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Nota.NotaEntity;

@Repository
public interface NotaRepository extends JpaRepository<NotaEntity, Long> {

    List<NotaEntity> findByAlunoId(Long alunoId);

    List<NotaEntity> findByAvaliacaoId(Long avaliacaoId);
}
