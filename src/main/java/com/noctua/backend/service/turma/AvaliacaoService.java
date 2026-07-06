package com.noctua.backend.service.turma;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Avaliacao.AvaliacaoRequestDTO;
import com.noctua.backend.dto.Avaliacao.AvaliacaoResponseDTO;
import com.noctua.backend.dto.Avaliacao.AvisoAvaliacaoPendenteDTO;
import com.noctua.backend.dto.Avaliacao.MediaPonderadaTurmaDTO;
import com.noctua.backend.dto.Dashboard.DashboardMetricasDTO;
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

    public List<NotaResponseDTO> listarTodasNotasPorTurma(Long turmaId) {
        turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        return notaRepository.findByAvaliacao_TurmaId(turmaId).stream()
                .map(this::toNotaResponseDTO)
                .toList();
    }

    public List<NotaResponseDTO> listarNotasPorAvaliacao(Long turmaId, Long avaliacaoId) {
        AvaliacaoEntity avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!avaliacao.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        return notaRepository.findByAvaliacaoId(avaliacaoId).stream()
                .sorted((a, b) -> a.getAluno().getNome().compareTo(b.getAluno().getNome()))
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
        dto.setTemChamadaFilha(avaliacaoRepository.existsByAvaliacaoPaiId(entity.getId()));
        avaliacaoRepository.findByAvaliacaoPaiId(entity.getId())
                .ifPresent(filha -> dto.setAvaliacaoFilhaId(filha.getId()));

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

    public AvaliacaoResponseDTO criarChamada(String emailProfessor, Long turmaId, Long avaliacaoId, LocalDate dataAplicacao) {
        buscarProfessorAutenticado(emailProfessor);

        AvaliacaoEntity pai = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        if (!pai.getTurma().getId().equals(turmaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não pertence à turma");
        }

        if (avaliacaoRepository.existsByAvaliacaoPaiId(avaliacaoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta avaliação já possui uma chamada subsequente.");
        }

        if (!Boolean.TRUE.equals(pai.getConcluida())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A avaliação precisa estar com status CONCLUÍDA para criar uma nova chamada.");
        }

        List<NotaEntity> todasNotas = notaRepository.findByAvaliacaoId(avaliacaoId);

        boolean temNotasPendentes = todasNotas.stream()
                .anyMatch(n -> n.getValor() == null && !Boolean.TRUE.equals(n.getNaoRealizada()));

        if (temNotasPendentes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Existem alunos com notas não preenchidas. Preencha todas as notas antes de criar uma nova chamada.");
        }

        List<NotaEntity> naoRealizadas = todasNotas.stream()
                .filter(n -> Boolean.TRUE.equals(n.getNaoRealizada()))
                .toList();

        if (naoRealizadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não existem alunos marcados como não compareceu para criar uma nova chamada.");
        }

        AvaliacaoEntity nova = new AvaliacaoEntity();
        nova.setTema(pai.getTema());
        nova.setData(dataAplicacao != null ? dataAplicacao.atStartOfDay() : pai.getData());
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

            List<NotaEntity> notasExistentes = notaRepository.findByAvaliacaoId(salva.getId());
            List<NotaEntity> comConflito = notasExistentes.stream()
                    .filter(n -> !request.getAlunosIds().contains(n.getAluno().getId()))
                    .filter(n -> n.getValor() != null || Boolean.TRUE.equals(n.getNaoRealizada()))
                    .toList();

            if (!comConflito.isEmpty()) {
                String alunosConflito = comConflito.stream()
                        .map(n -> n.getAluno().getId().toString())
                        .collect(Collectors.joining(", "));
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Não é possível remover alunos com nota já lançada ou marcada como não realizada: "
                                + alunosConflito);
            }

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

            List<NotaEntity> notasParaRemover = notaRepository.findByAvaliacaoId(salva.getId()).stream()
                    .filter(n -> !request.getAlunosIds().contains(n.getAluno().getId()))
                    .toList();
            notaRepository.deleteAll(notasParaRemover);

            List<NotaEntity> todasNotas = notaRepository.findByAvaliacaoId(salva.getId());
            boolean concluida = !todasNotas.isEmpty() && todasNotas.stream()
                    .allMatch(n -> Boolean.TRUE.equals(n.getNaoRealizada()) || n.getValor() != null);
            salva.setConcluida(concluida);
            avaliacaoRepository.save(salva);
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

        boolean temFilha = avaliacaoRepository.existsByAvaliacaoPaiId(avaliacaoId);
        if (temFilha) {
            if (Boolean.TRUE.equals(nota.getNaoRealizada()) && !Boolean.TRUE.equals(request.getNaoRealizada())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não é possível remover o status 'não compareceu' pois já existe uma chamada subsequente.");
            }
            if (Boolean.TRUE.equals(request.getNaoRealizada()) && nota.getValor() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não é possível marcar como 'não compareceu' um aluno que já possui nota lançada.");
            }
            if (Boolean.TRUE.equals(nota.getNaoRealizada()) && !Boolean.TRUE.equals(request.getNaoRealizada()) && request.getValor() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não é possível lançar nota para um aluno marcado como 'não compareceu'.");
            }
        }

        boolean naoRealizada = Boolean.TRUE.equals(request.getNaoRealizada());
        nota.setNaoRealizada(naoRealizada);
        nota.setValor(naoRealizada ? BigDecimal.ZERO : request.getValor());
        notaRepository.save(nota);

        List<NotaEntity> todasNotas = notaRepository.findByAvaliacaoId(avaliacaoId);
        boolean concluida = !todasNotas.isEmpty() && todasNotas.stream()
                .allMatch(n -> Boolean.TRUE.equals(n.getNaoRealizada()) || n.getValor() != null);
        avaliacao.setConcluida(concluida);
        avaliacaoRepository.save(avaliacao);

        return toNotaResponseDTO(nota);
    }

    public BigDecimal calcularMediaPonderada(Long idAluno, Integer numeroPeriodo) {
        AlunoEntity aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));

        Long idTurma = aluno.getTurma().getId();

        List<AvaliacaoEntity> todasAvaliacoes = avaliacaoRepository.findByTurmaId(idTurma);
        List<NotaEntity> notasAluno = notaRepository.findByAlunoIdAndAvaliacao_TurmaId(idAluno, idTurma);

        List<AvaliacaoEntity> rootNoPeriodo = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() == null && a.getPeriodo().equals(numeroPeriodo))
                .toList();

        Map<Long, AvaliacaoEntity> filhaPorPaiId = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() != null)
                .collect(Collectors.toMap(a -> a.getAvaliacaoPai().getId(), a -> a, (x, y) -> y));

        Map<String, NotaEntity> notaMap = notasAluno.stream()
                .collect(Collectors.toMap(
                        n -> n.getAluno().getId() + ":" + n.getAvaliacao().getId(),
                        n -> n,
                        (x, y) -> y));

        return calcularMediaAlunoNoPeriodo(idAluno, numeroPeriodo, rootNoPeriodo, filhaPorPaiId, notaMap);
    }

    public MediaPonderadaTurmaDTO calcularMediaPonderadaTurma(Long idTurma) {
        turmaRepository.findById(idTurma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        List<AvaliacaoEntity> todasAvaliacoes = avaliacaoRepository.findByTurmaId(idTurma);
        List<NotaEntity> todasNotas = notaRepository.findByAvaliacao_TurmaId(idTurma);
        List<AlunoEntity> alunos = alunoRepository.findByTurmaIdAndAtivo(idTurma, true);

        List<AvaliacaoEntity> rootAvaliacoes = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() == null)
                .toList();

        Map<Long, AvaliacaoEntity> filhaPorPaiId = todasAvaliacoes.stream()
                .filter(a -> a.getAvaliacaoPai() != null)
                .collect(Collectors.toMap(a -> a.getAvaliacaoPai().getId(), a -> a, (x, y) -> y));

        Map<String, NotaEntity> notaMap = todasNotas.stream()
                .collect(Collectors.toMap(
                        n -> n.getAluno().getId() + ":" + n.getAvaliacao().getId(),
                        n -> n,
                        (x, y) -> y));

        TreeSet<Integer> periodos = rootAvaliacoes.stream()
                .map(AvaliacaoEntity::getPeriodo)
                .collect(Collectors.toCollection(TreeSet::new));

        List<MediaPonderadaTurmaDTO.MediaAlunoDTO> mediasAlunos = alunos.stream()
                .sorted(Comparator.comparing(AlunoEntity::getNome))
                .map(aluno -> {
                    Map<Integer, BigDecimal> mediaPorPeriodo = new LinkedHashMap<>();
                    for (Integer periodo : periodos) {
                        mediaPorPeriodo.put(periodo, calcularMediaAlunoNoPeriodo(
                                aluno.getId(), periodo, rootAvaliacoes, filhaPorPaiId, notaMap));
                    }
                    return new MediaPonderadaTurmaDTO.MediaAlunoDTO(aluno.getId(), aluno.getNome(), mediaPorPeriodo);
                })
                .toList();

        Map<Integer, MediaPonderadaTurmaDTO.MediaResumoPeriodoDTO> resumoPorPeriodo = new LinkedHashMap<>();
        for (Integer periodo : periodos) {
            List<AvaliacaoEntity> avaliacoesNoPeriodo = rootAvaliacoes.stream()
                    .filter(a -> a.getPeriodo().equals(periodo))
                    .toList();

            resumoPorPeriodo.put(periodo, new MediaPonderadaTurmaDTO.MediaResumoPeriodoDTO(
                    calcularMediaTurmaDoTipo(alunos, avaliacoesNoPeriodo, TipoAvaliacao.PROVA, filhaPorPaiId, notaMap),
                    calcularMediaTurmaDoTipo(alunos, avaliacoesNoPeriodo, TipoAvaliacao.TRABALHO, filhaPorPaiId, notaMap),
                    calcularMediaTurmaDoTipo(alunos, avaliacoesNoPeriodo, TipoAvaliacao.ATIVIDADE, filhaPorPaiId, notaMap)));
        }

        return new MediaPonderadaTurmaDTO(mediasAlunos, resumoPorPeriodo);
    }

    private BigDecimal resolverNotaEfetiva(
            Long alunoId,
            AvaliacaoEntity avaliacao,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap) {

        NotaEntity nota = notaMap.get(alunoId + ":" + avaliacao.getId());
        if (nota == null) {
            return null;
        }

        if (Boolean.TRUE.equals(nota.getNaoRealizada())) {
            // Segunda chamada tem prioridade sobre o não comparecimento
            AvaliacaoEntity filha = filhaPorPaiId.get(avaliacao.getId());
            if (filha != null) {
                NotaEntity notaFilha = notaMap.get(alunoId + ":" + filha.getId());
                if (notaFilha != null && Boolean.FALSE.equals(notaFilha.getNaoRealizada()) && notaFilha.getValor() != null) {
                    return notaFilha.getValor();
                }
            }
            // Sem 2ª chamada válida: conta como 0 (valor já é 0 em registros novos; compat. com registros antigos que têm valor null)
            return nota.getValor() != null ? nota.getValor() : BigDecimal.ZERO;
        }

        // Nota regular com valor lançado
        if (nota.getValor() != null) {
            return nota.getValor();
        }

        // Nota da avaliação pai ainda não lançada; verifica se há 2ª chamada com nota
        AvaliacaoEntity filha = filhaPorPaiId.get(avaliacao.getId());
        if (filha != null) {
            NotaEntity notaFilha = notaMap.get(alunoId + ":" + filha.getId());
            if (notaFilha != null && Boolean.FALSE.equals(notaFilha.getNaoRealizada()) && notaFilha.getValor() != null) {
                return notaFilha.getValor();
            }
        }

        return null;
    }

    private BigDecimal calcularMediaAlunoNoPeriodo(
            Long alunoId,
            Integer periodo,
            List<AvaliacaoEntity> rootAvaliacoes,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap) {

        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesos = BigDecimal.ZERO;

        for (AvaliacaoEntity avaliacao : rootAvaliacoes) {
            if (!avaliacao.getPeriodo().equals(periodo)) continue;

            BigDecimal notaEfetiva = resolverNotaEfetiva(alunoId, avaliacao, filhaPorPaiId, notaMap);
            if (notaEfetiva != null) {
                BigDecimal peso = BigDecimal.valueOf(avaliacao.getPeso());
                somaPonderada = somaPonderada.add(notaEfetiva.multiply(peso));
                somaPesos = somaPesos.add(peso);
            }
        }

        return somaPesos.compareTo(BigDecimal.ZERO) > 0
                ? somaPonderada.divide(somaPesos, 2, RoundingMode.HALF_UP)
                : null;
    }

    private BigDecimal calcularMediaTurmaDoTipo(
            List<AlunoEntity> alunos,
            List<AvaliacaoEntity> avaliacoesNoPeriodo,
            TipoAvaliacao tipo,
            Map<Long, AvaliacaoEntity> filhaPorPaiId,
            Map<String, NotaEntity> notaMap) {

        List<AvaliacaoEntity> doTipo = avaliacoesNoPeriodo.stream()
                .filter(a -> tipo.equals(a.getTipo()))
                .toList();

        if (doTipo.isEmpty()) return null;

        BigDecimal soma = BigDecimal.ZERO;
        BigDecimal totalPesos = BigDecimal.ZERO;

        for (AlunoEntity aluno : alunos) {
            for (AvaliacaoEntity avaliacao : doTipo) {
                BigDecimal nota = resolverNotaEfetiva(aluno.getId(), avaliacao, filhaPorPaiId, notaMap);
                if (nota != null) {
                    BigDecimal peso = BigDecimal.valueOf(avaliacao.getPeso());
                    soma = soma.add(nota.multiply(peso));
                    totalPesos = totalPesos.add(peso);
                }
            }
        }

        return totalPesos.compareTo(BigDecimal.ZERO) > 0
                ? soma.divide(totalPesos, 2, RoundingMode.HALF_UP)
                : null;
    }

    public List<AvisoAvaliacaoPendenteDTO> listarAvisosAvaliacoesPendentes(String emailProfessor) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(14);
        System.out.println(LocalDateTime.now());

        List<AvaliacaoEntity> avaliacoes = avaliacaoRepository
                .findAvaliacoesPendentesComNotasPorProfessor(professor.getId(), dataLimite);

        return avaliacoes.stream().map(a -> {
            int alunosSemNota = (int) notaRepository.findByAvaliacaoId(a.getId()).stream()
                    .filter(n -> n.getValor() == null)
                    .count();

            AvisoAvaliacaoPendenteDTO dto = new AvisoAvaliacaoPendenteDTO();
            dto.setAvaliacaoId(a.getId());
            dto.setTurmaId(a.getTurma().getId());
            dto.setTurmaNome(a.getTurma().getNome());
            dto.setTema(a.getTema());
            dto.setTipo(a.getTipo());
            dto.setDataAplicacao(a.getData());
            dto.setDiasPendentes(ChronoUnit.DAYS.between(a.getData().toLocalDate(), LocalDate.now()));
            dto.setAlunosSemNota(alunosSemNota);
            return dto;
        }).toList();
    }

    public DashboardMetricasDTO getMetricasDashboard(String emailProfessor) {
        ProfessorEntity professor = buscarProfessorAutenticado(emailProfessor);
        long totalTurmas = turmaRepository.countByProfessorIdAndAtivoTrue(professor.getId());
        long totalAlunos = alunoRepository.countAtivosByProfessorId(professor.getId());
        long totalAvaliacoes = avaliacaoRepository.countByProfessorId(professor.getId());
        return new DashboardMetricasDTO(totalAlunos, totalTurmas, totalAvaliacoes);
    }
}
