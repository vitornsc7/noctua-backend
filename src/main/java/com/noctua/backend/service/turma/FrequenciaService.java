package com.noctua.backend.service.turma;

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

        FrequenciaEntity frequencia = new FrequenciaEntity();
        frequencia.setDataFalta(request.getDataFalta());
        frequencia.setPeriodo(request.getPeriodo());
        frequencia.setAluno(aluno);

        FrequenciaEntity salva = frequenciaRepository.save(frequencia);

        return converterParaResponse(salva);
    }

    public List<FrequenciaResponseDTO> listarPorAluno(Long alunoId) {
        buscarAluno(alunoId);

        return frequenciaRepository.findByAlunoId(alunoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public List<FrequenciaResponseDTO> listarPorTurma(Long turmaId) {
        return frequenciaRepository.findByAluno_TurmaId(turmaId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public FrequenciaResponseDTO atualizarFalta(Long id, FrequenciaRequestDTO request) {
        FrequenciaEntity frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de falta não encontrado."));

        AlunoEntity aluno = buscarAluno(request.getAlunoId());

        validarPeriodo(request.getPeriodo(), aluno);

        frequencia.setDataFalta(request.getDataFalta());
        frequencia.setPeriodo(request.getPeriodo());
        frequencia.setAluno(aluno);

        FrequenciaEntity atualizada = frequenciaRepository.save(frequencia);

        return converterParaResponse(atualizada);
    }

    public void excluirFalta(Long id) {
        if (!frequenciaRepository.existsById(id)) {
            throw new RuntimeException("Registro de falta não encontrado.");
        }

        frequenciaRepository.deleteById(id);
    }

    public double calcularPercentualFrequencia(Long alunoId, Integer periodo) {
        AlunoEntity aluno = buscarAluno(alunoId);

        validarPeriodo(periodo, aluno);

        Integer aulasPrevistas = aluno.getTurma().getQtdeAulasPrevistasPeriodo();

        long totalFaltas = frequenciaRepository.countByAlunoIdAndPeriodo(alunoId, periodo);

        if (aulasPrevistas == null || aulasPrevistas <= 0) {
            return 0.0;
        }

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

    private FrequenciaResponseDTO converterParaResponse(FrequenciaEntity frequencia) {
        return new FrequenciaResponseDTO(
                frequencia.getId(),
                frequencia.getDataFalta(),
                frequencia.getPeriodo(),
                frequencia.getAluno().getId());
    }
}