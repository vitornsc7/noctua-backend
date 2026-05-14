package com.noctua.backend.repository.turma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;

@Repository
public interface AvaliacaoRepository extends JpaRepository<AvaliacaoEntity, Long>, JpaSpecificationExecutor<AvaliacaoEntity> {

    List<AvaliacaoEntity> findByTurmaId(Long turmaId);

    boolean existsByAvaliacaoPaiId(Long avaliacaoPaiId);

    Optional<AvaliacaoEntity> findByAvaliacaoPaiId(Long avaliacaoPaiId);
}
