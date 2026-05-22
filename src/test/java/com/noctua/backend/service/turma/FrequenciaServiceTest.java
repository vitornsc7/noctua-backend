package com.noctua.backend.service.turma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.noctua.backend.dto.Frequencia.FrequenciaRequestDTO;
import com.noctua.backend.dto.Frequencia.FrequenciaResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;

@ExtendWith(MockitoExtension.class)
class FrequenciaServiceTest {

    @Mock
    private FrequenciaRepository frequenciaRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @InjectMocks
    private FrequenciaService frequenciaService;

    // Teste 1: registra falta ativa para aluno existente e retorna response DTO.
    @Test
    void registrarFaltaDeveSalvarFrequenciaAtivaQuandoDadosForemValidos() {
        TurmaEntity turma = criarTurma(10L, 4, 20);
        AlunoEntity aluno = criarAluno(100L, turma);
        FrequenciaRequestDTO request = criarRequest(100L, 2, 3);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(frequenciaRepository.save(any(FrequenciaEntity.class))).thenAnswer(invocation -> {
            FrequenciaEntity frequencia = invocation.getArgument(0);
            frequencia.setId(50L);
            return frequencia;
        });

        FrequenciaResponseDTO response = frequenciaService.registrarFalta(request);

        assertEquals(50L, response.getId());
        assertEquals(2, response.getPeriodo());
        assertEquals(3, response.getPeriodosFaltados());
        assertEquals(100L, response.getAlunoId());

        ArgumentCaptor<FrequenciaEntity> frequenciaCaptor = ArgumentCaptor.forClass(FrequenciaEntity.class);
        verify(frequenciaRepository).save(frequenciaCaptor.capture());
        assertEquals(true, frequenciaCaptor.getValue().getAtivo());
        assertEquals(aluno, frequenciaCaptor.getValue().getAluno());
    }

    // Teste 2: não registra falta quando o aluno não existe.
    @Test
    void registrarFaltaDeveLancarErroQuandoAlunoNaoForEncontrado() {
        when(alunoRepository.findById(100L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> frequenciaService.registrarFalta(criarRequest(100L, 1, 1)));

        assertEquals("Aluno não encontrado.", exception.getMessage());
        verify(frequenciaRepository, never()).save(any());
    }

    // Teste 3: não registra falta com periodo fora da quantidade de períodos da turma.
    @Test
    void registrarFaltaDeveLancarErroQuandoPeriodoForInvalidoParaTurma() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> frequenciaService.registrarFalta(criarRequest(100L, 5, 1)));

        assertEquals("Período inválido para a turma do aluno.", exception.getMessage());
        verify(frequenciaRepository, never()).save(any());
    }

    // Teste 4: não registra falta com data futura.
    @Test
    void registrarFaltaDeveLancarErroQuandoDataForFutura() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaRequestDTO request = new FrequenciaRequestDTO(
                LocalDate.now().plusDays(1).atStartOfDay(),
                1,
                100L,
                1);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> frequenciaService.registrarFalta(request));

        assertEquals("A data da falta não pode ser futura.", exception.getMessage());
        verify(frequenciaRepository, never()).save(any());
    }

    // Teste 5: lista apenas faltas ativas de um aluno existente.
    @Test
    void listarPorAlunoDeveRetornarFaltasAtivasDoAluno() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity frequencia = criarFrequencia(50L, aluno, 1, 2, true);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(frequencia));

        List<FrequenciaResponseDTO> response = frequenciaService.listarPorAluno(100L);

        assertEquals(1, response.size());
        assertEquals(50L, response.get(0).getId());
        assertEquals(2, response.get(0).getPeriodosFaltados());
    }

    // Teste 6: lista faltas por turma usando filtros e pageable.
    @Test
    void listarPorTurmaDeveRetornarPaginaDeFrequenciasComFiltros() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity frequencia = criarFrequencia(50L, aluno, 1, 2, true);
        Pageable pageable = PageRequest.of(0, 10);

        when(frequenciaRepository.findAll(org.mockito.ArgumentMatchers.<Specification<FrequenciaEntity>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(frequencia), pageable, 1));

        Page<FrequenciaResponseDTO> response = frequenciaService.listarPorTurma(
                10L,
                1,
                LocalDate.now(),
                100L,
                pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(100L, response.getContent().get(0).getAlunoId());
        verify(frequenciaRepository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<FrequenciaEntity>>any(), eq(pageable));
    }

    // Teste 7: atualiza falta ativa quando o registro e o aluno existem.
    @Test
    void atualizarFaltaDeveAlterarRegistroAtivo() {
        TurmaEntity turma = criarTurma(10L, 4, 20);
        AlunoEntity aluno = criarAluno(100L, turma);
        FrequenciaEntity frequencia = criarFrequencia(50L, aluno, 1, 1, true);
        FrequenciaRequestDTO request = criarRequest(100L, 2, 3);

        when(frequenciaRepository.findById(50L)).thenReturn(Optional.of(frequencia));
        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(frequenciaRepository.save(frequencia)).thenReturn(frequencia);

        FrequenciaResponseDTO response = frequenciaService.atualizarFalta(50L, request);

        assertEquals(2, response.getPeriodo());
        assertEquals(3, response.getPeriodosFaltados());
        verify(frequenciaRepository).save(frequencia);
    }

    // Teste 8: não atualiza falta quando o registro já está inativo.
    @Test
    void atualizarFaltaDeveLancarErroQuandoRegistroEstiverInativo() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity frequencia = criarFrequencia(50L, aluno, 1, 1, false);

        when(frequenciaRepository.findById(50L)).thenReturn(Optional.of(frequencia));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> frequenciaService.atualizarFalta(50L, criarRequest(100L, 1, 1)));

        assertEquals("Registro de falta não encontrado.", exception.getMessage());
        verify(frequenciaRepository, never()).save(any());
    }

    // Teste 9: excluir falta faz exclusão lógica marcando o registro como inativo.
    @Test
    void excluirFaltaDeveMarcarRegistroComoInativo() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity frequencia = criarFrequencia(50L, aluno, 1, 1, true);

        when(frequenciaRepository.findById(50L)).thenReturn(Optional.of(frequencia));

        frequenciaService.excluirFalta(50L);

        assertFalse(frequencia.getAtivo());
        verify(frequenciaRepository).save(frequencia);
    }

    // Teste 10: calcula percentual considerando faltas do período informado.
    @Test
    void calcularPercentualFrequenciaDeveConsiderarFaltasDoPeriodo() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity faltaPeriodo1 = criarFrequencia(50L, aluno, 1, 3, true);
        FrequenciaEntity outraFaltaPeriodo1 = criarFrequencia(51L, aluno, 1, 2, true);
        FrequenciaEntity faltaPeriodo2 = criarFrequencia(52L, aluno, 2, 4, true);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L))
                .thenReturn(List.of(faltaPeriodo1, outraFaltaPeriodo1, faltaPeriodo2));

        double percentual = frequenciaService.calcularPercentualFrequencia(100L, 1);

        assertEquals(75.0, percentual);
    }

    // Teste 11: classifica frequência como critica quando percentual fica abaixo de 75%.
    @Test
    void classificarFrequenciaDeveRetornarCriticaQuandoPercentualForBaixo() {
        AlunoEntity aluno = criarAluno(100L, criarTurma(10L, 4, 20));
        FrequenciaEntity falta = criarFrequencia(50L, aluno, 1, 6, true);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(frequenciaRepository.findByAlunoIdAndAtivoTrue(100L)).thenReturn(List.of(falta));

        String classificacao = frequenciaService.classificarFrequencia(100L, 1);

        assertEquals("Crítica", classificacao);
    }

    private FrequenciaRequestDTO criarRequest(Long alunoId, Integer periodo, Integer periodosFaltados) {
        return new FrequenciaRequestDTO(
                LocalDate.now().atStartOfDay(),
                periodo,
                alunoId,
                periodosFaltados);
    }

    private TurmaEntity criarTurma(Long id, Integer qtdePeriodos, Integer aulasPrevistas) {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(id);
        turma.setNome("Turma A");
        turma.setQtdePeriodos(qtdePeriodos);
        turma.setQtdeAulasPrevistasPeriodo(aulasPrevistas);
        turma.setAtivo(true);
        return turma;
    }

    private AlunoEntity criarAluno(Long id, TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(id);
        aluno.setNome("Ana");
        aluno.setAtivo(true);
        aluno.setTurma(turma);
        return aluno;
    }

    private FrequenciaEntity criarFrequencia(
            Long id,
            AlunoEntity aluno,
            Integer periodo,
            Integer periodosFaltados,
            Boolean ativo) {
        FrequenciaEntity frequencia = new FrequenciaEntity();
        frequencia.setId(id);
        frequencia.setDataFalta(LocalDateTime.now().minusDays(1));
        frequencia.setPeriodo(periodo);
        frequencia.setPeriodosFaltados(periodosFaltados);
        frequencia.setAtivo(ativo);
        frequencia.setAluno(aluno);
        return frequencia;
    }
}
