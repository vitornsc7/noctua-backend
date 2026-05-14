package com.noctua.backend.service.turma;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Nota.NotaRequestDTO;
import com.noctua.backend.dto.Nota.NotaResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;
import com.noctua.backend.entity.Nota.NotaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.enums.TipoAvaliacao;
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
        entity.setNumeroChamada(1);
        entity.setAvaliacaoPai(null);

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

    public Page<AvaliacaoResponseDTO> listarPorTurma(Long turmaId, Integer periodo, TipoAvaliacao tipo, Boolean concluida, Pageable pageable) {
        turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        Specification<AvaliacaoEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("turma").get("id"), turmaId));
            if (periodo != null) {
                predicates.add(cb.equal(root.get("periodo"), periodo));
            }
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            if (concluida != null) {
                predicates.add(cb.equal(root.get("concluida"), concluida));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return avaliacaoRepository.findAll(spec, pageable).map(this::toResponseDTO);
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
        dto.setNumeroChamada(entity.getNumeroChamada());
        dto.setAvaliacaoPaiId(entity.getAvaliacaoPai() != null ? entity.getAvaliacaoPai().getId() : null);

        List<NotaEntity> notas = notaRepository.findByAvaliacaoId(entity.getId());
        dto.setNotasCount(notas.size());
        dto.setConcluida(Boolean.TRUE.equals(entity.getConcluida()));

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

    public AvaliacaoResponseDTO criarChamada(String emailProfessor, Long turmaId, Long avaliacaoId) {
        buscarProfessorAutenticado(emailProfessor);

        AvaliacaoEntity pai = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!pai.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        List<NotaEntity> naoRealizadas = notaRepository.findByAvaliacaoId(avaliacaoId).stream()
                .filter(n -> Boolean.TRUE.equals(n.getNaoRealizada()))
                .toList();

        if (naoRealizadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não há alunos marcados como não compareceu para criar uma nova chamada.");
        }

        AvaliacaoEntity nova = new AvaliacaoEntity();
        nova.setTema(pai.getTema());
        nova.setData(pai.getData());
        nova.setPeso(pai.getPeso());
        nova.setTipo(pai.getTipo());
        nova.setPeriodo(pai.getPeriodo());
        nova.setTurma(pai.getTurma());
        nova.setNumeroChamada(pai.getNumeroChamada() + 1);
        nova.setAvaliacaoPai(pai);

        AvaliacaoEntity salva = avaliacaoRepository.save(nova);

        for (NotaEntity notaPai : naoRealizadas) {
            NotaEntity nota = new NotaEntity();
            nota.setValor(null);
            nota.setNaoRealizada(false);
            nota.setAvaliacao(salva);
            nota.setAluno(notaPai.getAluno());
            notaRepository.save(nota);
        }

        return toResponseDTO(salva);
    }

    public AvaliacaoResponseDTO atualizar(Long turmaId, Long avaliacaoId, AvaliacaoRequestDTO request) {
        AvaliacaoEntity avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!avaliacao.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        avaliacao.setTema(request.getTema());
        avaliacao.setData(request.getData());
        avaliacao.setPeso(request.getPeso());
        avaliacao.setTipo(request.getTipo());
        avaliacao.setPeriodo(request.getPeriodo());

        AvaliacaoEntity salva = avaliacaoRepository.save(avaliacao);

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

    public NotaResponseDTO atualizarNota(Long turmaId, Long avaliacaoId, Long notaId, NotaRequestDTO request) {
        AvaliacaoEntity avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!avaliacao.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        NotaEntity nota = notaRepository.findById(notaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada"));

        if (!nota.getAvaliacao().getId().equals(avaliacaoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não pertence à avaliação");
        }

        boolean naoRealizada = Boolean.TRUE.equals(request.getNaoRealizada());
        nota.setNaoRealizada(naoRealizada);
        nota.setValor(naoRealizada ? null : request.getValor());
        notaRepository.save(nota);

        List<NotaEntity> todasNotas = notaRepository.findByAvaliacaoId(avaliacaoId);
        boolean concluida = !todasNotas.isEmpty() && todasNotas.stream()
                .allMatch(n -> Boolean.TRUE.equals(n.getNaoRealizada()) || n.getValor() != null);
        avaliacao.setConcluida(concluida);
        avaliacaoRepository.save(avaliacao);

        return toNotaResponseDTO(nota);
    }
}
