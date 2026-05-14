package com.noctua.backend.service.turma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Frequencia.FrequenciaRequestDTO;
import com.noctua.backend.dto.Frequencia.FrequenciaResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FrequenciaService {

    private final FrequenciaRepository frequenciaRepository;
    private final AlunoRepository alunoRepository;

    public FrequenciaResponseDTO registrarFalta(FrequenciaRequestDTO request) {
        AlunoEntity aluno = buscarAluno(request.getAlunoId());

        validarPeriodo(request.getPeriodo(), aluno);
        validarPeriodosFaltados(request.getPeriodosFaltados());
        validarDataFalta(request.getDataFalta());

        FrequenciaEntity frequencia = new FrequenciaEntity();
        frequencia.setDataFalta(request.getDataFalta());
        frequencia.setPeriodo(request.getPeriodo());
        frequencia.setPeriodosFaltados(request.getPeriodosFaltados());
        frequencia.setAtivo(true);
        frequencia.setAluno(aluno);

        FrequenciaEntity salva = frequenciaRepository.save(frequencia);

        return converterParaResponse(salva);
    }

    public List<FrequenciaResponseDTO> listarPorAluno(Long alunoId) {
        buscarAluno(alunoId);

        return frequenciaRepository.findByAlunoIdAndAtivoTrue(alunoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public List<FrequenciaResponseDTO> listarPorTurma(Long turmaId, Integer periodo, LocalDate dataFalta) {
        List<FrequenciaEntity> frequencias;
        if (periodo != null && dataFalta != null) {
            frequencias = frequenciaRepository.findByAluno_TurmaIdAndPeriodoAndDataFaltaAndAtivoTrue(turmaId, periodo, dataFalta);
        } else if (periodo != null) {
            frequencias = frequenciaRepository.findByAluno_TurmaIdAndPeriodoAndAtivoTrue(turmaId, periodo);
        } else if (dataFalta != null) {
            frequencias = frequenciaRepository.findByAluno_TurmaIdAndDataFaltaAndAtivoTrue(turmaId, dataFalta);
        } else {
            frequencias = frequenciaRepository.findByAluno_TurmaIdAndAtivoTrue(turmaId);
        }
        return frequencias.stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public FrequenciaResponseDTO atualizarFalta(Long id, FrequenciaRequestDTO request) {
        FrequenciaEntity frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de falta não encontrado."));

        if (Boolean.FALSE.equals(frequencia.getAtivo())) {
            throw new RuntimeException("Registro de falta não encontrado.");
        }

        AlunoEntity aluno = buscarAluno(request.getAlunoId());
        validarDataFalta(request.getDataFalta());

        validarPeriodo(request.getPeriodo(), aluno);
        validarPeriodosFaltados(request.getPeriodosFaltados());

        frequencia.setDataFalta(request.getDataFalta());
        frequencia.setPeriodo(request.getPeriodo());
        frequencia.setPeriodosFaltados(request.getPeriodosFaltados());
        frequencia.setAluno(aluno);

        FrequenciaEntity atualizada = frequenciaRepository.save(frequencia);

        return converterParaResponse(atualizada);
    }

    public void excluirFalta(Long id) {
        FrequenciaEntity frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de falta não encontrado."));

        if (Boolean.FALSE.equals(frequencia.getAtivo())) {
            throw new RuntimeException("Registro de falta não encontrado.");
        }

        frequencia.setAtivo(false);
        frequenciaRepository.save(frequencia);
    }

    public double calcularPercentualFrequencia(Long alunoId, Integer periodo) {
        AlunoEntity aluno = buscarAluno(alunoId);

        validarPeriodo(periodo, aluno);

        Integer aulasPrevistas = aluno.getTurma().getQtdeAulasPrevistasPeriodo();

        if (aulasPrevistas == null || aulasPrevistas <= 0) {
            return 0.0;
        }

        List<FrequenciaEntity> faltas = frequenciaRepository.findByAlunoIdAndAtivoTrue(alunoId);

        int totalFaltas = faltas.stream()
                .filter(falta -> falta.getPeriodo().equals(periodo))
                .mapToInt(falta -> falta.getPeriodosFaltados() != null ? falta.getPeriodosFaltados() : 1)
                .sum();

        double frequencia = ((aulasPrevistas - totalFaltas) * 100.0) / aulasPrevistas;

        return Math.max(frequencia, 0.0);
    }

    public String classificarFrequencia(Long alunoId, Integer periodo) {
        double percentual = calcularPercentualFrequencia(alunoId, periodo);

        if (percentual >= 90)
            return "Alta";
        if (percentual >= 80)
            return "Regular";
        if (percentual >= 75)
            return "Atenção";
        return "Crítica";
    }

    private AlunoEntity buscarAluno(Long alunoId) {
        return alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));
    }

    private void validarPeriodo(Integer periodo, AlunoEntity aluno) {
        if (periodo == null) {
            throw new RuntimeException("O período é obrigatório.");
        }

        Integer qtdePeriodos = aluno.getTurma().getQtdePeriodos();

        if (periodo < 1 || periodo > qtdePeriodos) {
            throw new RuntimeException("Período inválido para a turma do aluno.");
        }
    }

    private void validarPeriodosFaltados(Integer periodosFaltados) {
        if (periodosFaltados == null) {
            throw new RuntimeException("A quantidade de períodos faltados é obrigatória.");
        }

        if (periodosFaltados < 1 || periodosFaltados > 6) {
            throw new RuntimeException("A quantidade de períodos faltados deve estar entre 1 e 6.");
        }
    }

    private FrequenciaResponseDTO converterParaResponse(FrequenciaEntity frequencia) {
        return new FrequenciaResponseDTO(
                frequencia.getId(),
                frequencia.getDataFalta(),
                frequencia.getPeriodo(),
                frequencia.getPeriodosFaltados(),
                frequencia.getAluno().getId());
    }

    private void validarDataFalta(LocalDateTime dataFalta) {
        if (dataFalta == null) {
            throw new RuntimeException("A data da falta é obrigatória.");
        }

        LocalDate dataInformada = dataFalta.toLocalDate();
        LocalDate hojeBrasil = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        if (dataInformada.isAfter(hojeBrasil)) {
            throw new RuntimeException("A data da falta não pode ser futura.");
        }
    }
}