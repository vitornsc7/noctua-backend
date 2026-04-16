package com.noctua.backend.service.turma;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.dto.Turma.TurmaFiltrosDTO;
import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.TurmaRepository;

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
        entity.setInstituicao(request.getInstituicao());

        TurmaEntity salva = turmaRepository.save(entity);
        return toResponseDTO(salva);
    }

    public Page<TurmaResponseDTO> listar(Pageable pageable, String turno, String anoLetivo, String instituicao) {
        Specification<TurmaEntity> spec = Specification.where(null);

        if (turno != null && !turno.isBlank()) {
            Turno turnoEnum = Turno.valueOf(turno.toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("turno"), turnoEnum));
        }

        if (anoLetivo != null && !anoLetivo.isBlank()) {
            int ano = Integer.parseInt(anoLetivo);
            LocalDate start = LocalDate.of(ano, 1, 1);
            LocalDate end = LocalDate.of(ano, 12, 31);
            spec = spec.and((root, query, cb) -> cb.between(root.get("anoLetivo"), start, end));
        }

        if (instituicao != null && !instituicao.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("instituicao"), instituicao));
        }

        return turmaRepository.findAll(spec, pageable).map(this::toResponseDTO);
    }

    public TurmaFiltrosDTO buscarFiltros() {
        return new TurmaFiltrosDTO(
                turmaRepository.findDistinctAnos(),
                turmaRepository.findDistinctInstituicoes());
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
        entity.setInstituicao(request.getInstituicao());

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
        dto.setInstituicao(entity.getInstituicao());

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
