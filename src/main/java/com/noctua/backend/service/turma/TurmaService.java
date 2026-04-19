package com.noctua.backend.service.turma;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.dto.Turma.TurmaFiltrosDTO;
import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.TurmaRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final ProfessorRepository professorRepository;

    public TurmaResponseDTO criar(String emailProfessor, TurmaRequestDTO request) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);

        TurmaEntity entity = new TurmaEntity();
        entity.setNome(request.getNome());
        entity.setAnoLetivo(request.getAnoLetivo());
        entity.setQtdePeriodos(request.getQtdePeriodos());
        entity.setQtdeAulasPrevistasPeriodo(request.getQtdeAulasPrevistasPeriodo());
        entity.setTurno(request.getTurno());
        entity.setDisciplina(request.getDisciplina());
        entity.setMediaMinima(request.getMediaMinima());
        entity.setInstituicao(request.getInstituicao());
        entity.setProfessor(professor);

        TurmaEntity salva = turmaRepository.save(entity);
        return toResponseDTO(salva);
    }

    public Page<TurmaResponseDTO> listar(String emailProfessor, Pageable pageable, String turno, String anoLetivo,
            String instituicao) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        Specification<TurmaEntity> spec = Specification.where(pertenceAoProfessor(professor.getId()));

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

    public TurmaFiltrosDTO buscarFiltros(String emailProfessor) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        return new TurmaFiltrosDTO(
                turmaRepository.findDistinctAnosByProfessorId(professor.getId()),
                turmaRepository.findDistinctInstituicoesByProfessorId(professor.getId()));
    }

    public TurmaResponseDTO buscarPorId(String emailProfessor, Long id) {
        TurmaEntity entity = buscarTurmaDoProfessor(emailProfessor, id);
        return toResponseDTO(entity);
    }

    public TurmaResponseDTO atualizar(String emailProfessor, Long id, TurmaRequestDTO request) {
        TurmaEntity entity = buscarTurmaDoProfessor(emailProfessor, id);

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

    public void deletar(String emailProfessor, Long id) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        if (!turmaRepository.existsByIdAndProfessorId(id, professor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada");
        }
        turmaRepository.deleteById(id);
    }

    private ProfessorEntity buscarProfessorAutenticado(String emailProfessor) {
        return professorRepository.findByUsuarioEmail(emailProfessor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor não encontrado"));
    }

    private TurmaEntity buscarTurmaDoProfessor(String emailProfessor, Long turmaId) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        return turmaRepository.findByIdAndProfessorId(turmaId, professor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
    }

    private Specification<TurmaEntity> pertenceAoProfessor(Long professorId) {
        return (root, query, cb) -> cb.equal(root.get("professor").get("id"), professorId);
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
