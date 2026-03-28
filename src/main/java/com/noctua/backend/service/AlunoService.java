package com.noctua.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Aluno.AlunoRequestDTO;
import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.repository.AlunoRepository;
import com.noctua.backend.repository.TurmaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final TurmaRepository turmaRepository;

    public AlunoResponseDTO criar(Long turmaId, AlunoRequestDTO request) {
        TurmaEntity turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada"));

        AlunoEntity entity = new AlunoEntity();
        entity.setNome(request.getNome());
        entity.setDescricao(request.getDescricao());
        entity.setAtivo(request.getAtivo() != null ? request.getAtivo() : true);
        entity.setTurma(turma);

        AlunoEntity salvo = alunoRepository.save(entity);
        return toResponseDTO(salvo);
    }

    public List<AlunoResponseDTO> listarPorTurma(Long turmaId) {
        return alunoRepository.findByTurmaId(turmaId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AlunoResponseDTO buscarPorId(Long id) {
        AlunoEntity entity = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        return toResponseDTO(entity);
    }

    public AlunoResponseDTO atualizar(Long id, AlunoRequestDTO request) {
        AlunoEntity entity = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

        entity.setNome(request.getNome());
        entity.setDescricao(request.getDescricao());
        if (request.getAtivo() != null) {
            entity.setAtivo(request.getAtivo());
        }

        AlunoEntity salvo = alunoRepository.save(entity);
        return toResponseDTO(salvo);
    }

    public void deletar(Long id) {
        if (!alunoRepository.existsById(id)) {
            throw new RuntimeException("Aluno não encontrado");
        }
        alunoRepository.deleteById(id);
    }

    private AlunoResponseDTO toResponseDTO(AlunoEntity entity) {
        AlunoResponseDTO dto = new AlunoResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setAtivo(entity.getAtivo());
        dto.setTurmaId(entity.getTurma().getId());
        return dto;
    }
}
