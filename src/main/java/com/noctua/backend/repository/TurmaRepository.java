package com.noctua.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.noctua.backend.entity.Turma.TurmaEntity;

@Repository
public interface TurmaRepository extends JpaRepository<TurmaEntity, Long>, JpaSpecificationExecutor<TurmaEntity> {
}
