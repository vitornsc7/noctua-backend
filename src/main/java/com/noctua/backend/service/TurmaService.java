package com.noctua.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.repository.TurmaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository turmaRepository;

    public TurmaResponseDTO criar(TurmaRequestDTO request) {
        TurmaEntity entity = new TurmaEntity();
        entity.setNome(request.getNome());
        entity.setAnoLetivo(request.getAnoLetivo());
        entity.setQtdePeriodos(request.getQtdePeriodos());
        entity.setQtdeAulasPrevistasPeriodo(request.getQtdeAulasPrevistasPeriodo());
        entity.setTurno(request.getTurno());
        entity.setDisciplina(request.getDisciplina());
        entity.setMediaMinima(request.getMediaMinima());

        TurmaEntity salva = turmaRepository.save(entity);
        return toResponseDTO(salva);
    }

    public List<TurmaResponseDTO> listarTodas() {
        return turmaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public TurmaResponseDTO buscarPorId(Long id) {
        TurmaEntity entity = turmaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada"));
        return toResponseDTO(entity);
    }

    public TurmaResponseDTO atualizar(Long id, TurmaRequestDTO request) {
        TurmaEntity entity = turmaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada"));

        entity.setNome(request.getNome());
        entity.setAnoLetivo(request.getAnoLetivo());
        entity.setQtdePeriodos(request.getQtdePeriodos());
        entity.setQtdeAulasPrevistasPeriodo(request.getQtdeAulasPrevistasPeriodo());
        entity.setTurno(request.getTurno());
        entity.setDisciplina(request.getDisciplina());
        entity.setMediaMinima(request.getMediaMinima());

        TurmaEntity salva = turmaRepository.save(entity);
        return toResponseDTO(salva);
    }

    public void deletar(Long id) {
        if (!turmaRepository.existsById(id)) {
            throw new RuntimeException("Turma não encontrada");
        }
        turmaRepository.deleteById(id);
    }

    private TurmaResponseDTO toResponseDTO(TurmaEntity entity) {
        TurmaResponseDTO dto = new TurmaResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setAnoLetivo(entity.getAnoLetivo());
        dto.setQtdePeriodos(entity.getQtdePeriodos());
        dto.setQtdeAulasPrevistasPeriodo(entity.getQtdeAulasPrevistasPeriodo());
        dto.setTurno(entity.getTurno());
        dto.setDisciplina(entity.getDisciplina());
        dto.setMediaMinima(entity.getMediaMinima());

        if (entity.getAlunos() != null) {
            List<AlunoResponseDTO> alunosDTO = entity.getAlunos().stream().map(aluno -> {
                AlunoResponseDTO alunoDTO = new AlunoResponseDTO();
                alunoDTO.setId(aluno.getId());
                alunoDTO.setNome(aluno.getNome());
                alunoDTO.setObservacao(aluno.getObservacao());
                alunoDTO.setAtivo(aluno.getAtivo());
                alunoDTO.setTurmaId(entity.getId());
                return alunoDTO;
            }).toList();
            dto.setAlunos(alunosDTO);
            dto.setAlunosCount(alunosDTO.size());
        } else {
            dto.setAlunos(List.of());
            dto.setAlunosCount(0);
        }

        return dto;
    }
}
