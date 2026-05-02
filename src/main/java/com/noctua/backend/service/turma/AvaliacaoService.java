package com.noctua.backend.service.turma;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Nota.NotaResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;
import com.noctua.backend.entity.Nota.NotaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.AvaliacaoRepository;
import com.noctua.backend.repository.turma.NotaRepository;
import com.noctua.backend.repository.turma.TurmaRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final NotaRepository notaRepository;
    private final ProfessorRepository professorRepository;

    public AvaliacaoResponseDTO criar(String emailProfessor, Long turmaId, AvaliacaoRequestDTO request) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        TurmaEntity turma = turmaRepository.findByIdAndProfessorIdAndAtivoTrue(turmaId, professor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        AvaliacaoEntity entity = new AvaliacaoEntity();
        entity.setTema(request.getTema());
        entity.setData(request.getData());
        entity.setPeso(request.getPeso());
        entity.setTipo(request.getTipo());
        entity.setPeriodo(request.getPeriodo());
        entity.setTurma(turma);

        AvaliacaoEntity salva = avaliacaoRepository.save(entity);

        if (request.getAlunosIds() != null) {
            for (Long alunoId : request.getAlunosIds()) {
                if (notaRepository.existsByAvaliacaoIdAndAlunoId(salva.getId(), alunoId)) {
                    continue;
                }
                AlunoEntity aluno = alunoRepository.findById(alunoId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Aluno não encontrado: " + alunoId));

                NotaEntity nota = new NotaEntity();
                nota.setValor(null);
                nota.setNaoRealizada(false);
                nota.setAvaliacao(salva);
                nota.setAluno(aluno);
                notaRepository.save(nota);
            }
        }

        return toResponseDTO(salva);
    }

    public List<AvaliacaoResponseDTO> listarPorTurma(Long turmaId) {
        turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        return avaliacaoRepository.findByTurmaId(turmaId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AvaliacaoResponseDTO buscarPorId(Long turmaId, Long avaliacaoId) {
        AvaliacaoEntity entity = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!entity.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        return toResponseDTO(entity);
    }

    public List<NotaResponseDTO> listarNotasPorAvaliacao(Long turmaId, Long avaliacaoId) {
        AvaliacaoEntity avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!avaliacao.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        return notaRepository.findByAvaliacaoId(avaliacaoId).stream()
                .sorted((a, b) -> {
                    if (a.getValor() == null && b.getValor() == null) return a.getAluno().getNome().compareTo(b.getAluno().getNome());
                    if (a.getValor() == null) return 1;
                    if (b.getValor() == null) return -1;
                    return b.getValor().compareTo(a.getValor());
                })
                .map(this::toNotaResponseDTO)
                .toList();
    }

    private ProfessorEntity buscarProfessorAutenticado(String emailProfessor) {
        return professorRepository.findByUsuarioEmail(emailProfessor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor não encontrado"));
    }

    private AvaliacaoResponseDTO toResponseDTO(AvaliacaoEntity entity) {
        AvaliacaoResponseDTO dto = new AvaliacaoResponseDTO();
        dto.setId(entity.getId());
        dto.setTema(entity.getTema());
        dto.setData(entity.getData());
        dto.setPeso(entity.getPeso());
        dto.setTipo(entity.getTipo());
        dto.setPeriodo(entity.getPeriodo());
        dto.setTurmaId(entity.getTurma().getId());

        List<NotaEntity> notas = notaRepository.findByAvaliacaoId(entity.getId());
        dto.setNotasCount(notas.size());

        List<BigDecimal> valores = notas.stream()
                .filter(n -> n.getValor() != null)
                .map(NotaEntity::getValor)
                .toList();

        if (!valores.isEmpty()) {
            BigDecimal sum = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setMedia(sum.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP));
        }

        return dto;
    }

    private NotaResponseDTO toNotaResponseDTO(NotaEntity entity) {
        NotaResponseDTO dto = new NotaResponseDTO();
        dto.setId(entity.getId());
        dto.setValor(entity.getValor());
        dto.setNaoRealizada(entity.getNaoRealizada());
        dto.setAvaliacaoId(entity.getAvaliacao().getId());
        dto.setAlunoId(entity.getAluno().getId());
        dto.setAlunoNome(entity.getAluno().getNome());
        return dto;
    }
}
