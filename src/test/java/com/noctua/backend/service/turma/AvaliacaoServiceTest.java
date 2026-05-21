package com.noctua.backend.service.turma;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.enums.TipoAvaliacao;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.AvaliacaoRepository;
import com.noctua.backend.repository.turma.NotaRepository;
import com.noctua.backend.repository.turma.TurmaRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    // Teste 1: cria avaliação e cria uma nota vazia para cada aluno informado.
    @Test
    void criarDeveSalvarAvaliacaoECriarNotasParaAlunosInformados() {
        ProfessorEntity professor = criarProfessor(1L);
        TurmaEntity turma = criarTurma(10L, professor);
        AlunoEntity aluno = criarAluno(100L, "Ana", turma);
        AvaliacaoRequestDTO request = criarRequest(List.of(100L));

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findByIdAndProfessorIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(turma));
        when(avaliacaoRepository.save(any(AvaliacaoEntity.class))).thenAnswer(invocation -> {
            AvaliacaoEntity avaliacao = invocation.getArgument(0);
            avaliacao.setId(50L);
            return avaliacao;
        });
        when(notaRepository.existsByAvaliacaoIdAndAlunoId(50L, 100L)).thenReturn(false);
        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(notaRepository.findByAvaliacaoId(50L)).thenReturn(List.of());
        when(avaliacaoRepository.existsByAvaliacaoPaiId(50L)).thenReturn(false);
        when(avaliacaoRepository.findByAvaliacaoPaiId(50L)).thenReturn(Optional.empty());

        AvaliacaoResponseDTO response = avaliacaoService.criar("prof@email.com", 10L, request);

        assertEquals(50L, response.getId());
        assertEquals("Prova 1", response.getTema());
        assertEquals(10L, response.getTurmaId());
        assertEquals(1, response.getNumeroChamada());

        ArgumentCaptor<NotaEntity> notaCaptor = ArgumentCaptor.forClass(NotaEntity.class);
        verify(notaRepository).save(notaCaptor.capture());
        assertNull(notaCaptor.getValue().getValor());
        assertFalse(notaCaptor.getValue().getNaoRealizada());
        assertEquals(aluno, notaCaptor.getValue().getAluno());
    }

    // Teste 2: professor inexistente --> bloqueia a criação antes de buscar a turma.
    @Test
    void criarDeveLancarForbiddenQuandoProfessorNaoForEncontrado() {
        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> avaliacaoService.criar("prof@email.com", 10L, criarRequest(List.of())));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(turmaRepository, never()).findByIdAndProfessorIdAndAtivoTrue(any(), any());
    }

    // Teste 3: buscarPorId não permite acessar avaliação de outra turma.
    @Test
    void buscarPorIdDeveLancarNotFoundQuandoAvaliacaoNaoPertencerATurma() {
        TurmaEntity outraTurma = criarTurma(99L, criarProfessor(1L));
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, outraTurma, TipoAvaliacao.PROVA, 1, 2);

        when(avaliacaoRepository.findById(50L)).thenReturn(Optional.of(avaliacao));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> avaliacaoService.buscarPorId(10L, 50L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Avaliação não pertence à turma", exception.getReason());
    }

    // Teste 4: listarNotasPorAvaliacao retorna notas ordenadas pelo nome do aluno.
    @Test
    void listarNotasPorAvaliacaoDeveRetornarNotasOrdenadasPorNomeDoAluno() {
        TurmaEntity turma = criarTurma(10L, criarProfessor(1L));
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, turma, TipoAvaliacao.PROVA, 1, 2);
        NotaEntity notaBia = criarNota(2L, avaliacao, criarAluno(2L, "Bia", turma), new BigDecimal("8.00"), false);
        NotaEntity notaAna = criarNota(1L, avaliacao, criarAluno(1L, "Ana", turma), new BigDecimal("9.00"), false);

        when(avaliacaoRepository.findById(50L)).thenReturn(Optional.of(avaliacao));
        when(notaRepository.findByAvaliacaoId(50L)).thenReturn(List.of(notaBia, notaAna));

        List<NotaResponseDTO> response = avaliacaoService.listarNotasPorAvaliacao(10L, 50L);

        assertEquals("Ana", response.get(0).getAlunoNome());
        assertEquals("Bia", response.get(1).getAlunoNome());
    }

    // Teste 5: atualizar nota salva valor e conclui a avaliação quando todas as notas estão preenchidas.
    @Test
    void atualizarNotaDeveSalvarNotaEConcluirAvaliacaoQuandoTodasNotasEstiveremPreenchidas() {
        TurmaEntity turma = criarTurma(10L, criarProfessor(1L));
        AvaliacaoEntity avaliacao = criarAvaliacao(50L, turma, TipoAvaliacao.PROVA, 1, 2);
        NotaEntity nota = criarNota(5L, avaliacao, criarAluno(100L, "Ana", turma), null, false);
        NotaRequestDTO request = new NotaRequestDTO(new BigDecimal("9.50"), false, 50L, 100L);

        when(avaliacaoRepository.findById(50L)).thenReturn(Optional.of(avaliacao));
        when(notaRepository.findById(5L)).thenReturn(Optional.of(nota));
        when(avaliacaoRepository.existsByAvaliacaoPaiId(50L)).thenReturn(false);
        when(notaRepository.findByAvaliacaoId(50L)).thenReturn(List.of(nota));

        NotaResponseDTO response = avaliacaoService.atualizarNota(10L, 50L, 5L, request);

        assertEquals(new BigDecimal("9.50"), response.getValor());
        assertFalse(response.getNaoRealizada());
        assertTrue(avaliacao.getConcluida());
        verify(notaRepository).save(nota);
        verify(avaliacaoRepository).save(avaliacao);
    }

    // Teste 6: calcula média ponderada do aluno no período usando os pesos das avaliações.
    @Test
    void calcularMediaPonderadaDeveRetornarMediaDoAlunoNoPeriodo() {
        TurmaEntity turma = criarTurma(10L, criarProfessor(1L));
        AlunoEntity aluno = criarAluno(100L, "Ana", turma);
        AvaliacaoEntity prova = criarAvaliacao(50L, turma, TipoAvaliacao.PROVA, 1, 2);
        AvaliacaoEntity trabalho = criarAvaliacao(51L, turma, TipoAvaliacao.TRABALHO, 1, 1);
        NotaEntity notaProva = criarNota(1L, prova, aluno, new BigDecimal("8.00"), false);
        NotaEntity notaTrabalho = criarNota(2L, trabalho, aluno, new BigDecimal("10.00"), false);

        when(alunoRepository.findById(100L)).thenReturn(Optional.of(aluno));
        when(avaliacaoRepository.findByTurmaId(10L)).thenReturn(List.of(prova, trabalho));
        when(notaRepository.findByAlunoIdAndAvaliacao_TurmaId(100L, 10L))
                .thenReturn(List.of(notaProva, notaTrabalho));

        BigDecimal media = avaliacaoService.calcularMediaPonderada(100L, 1);

        assertEquals(new BigDecimal("8.67"), media);
    }

    private AvaliacaoRequestDTO criarRequest(List<Long> alunosIds) {
        return new AvaliacaoRequestDTO(
                "Prova 1",
                LocalDateTime.of(2026, 5, 21, 10, 0),
                2,
                TipoAvaliacao.PROVA,
                1,
                10L,
                alunosIds);
    }

    private ProfessorEntity criarProfessor(Long id) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(id)
                .nome("Professor")
                .email("prof@email.com")
                .senhaHash("hash")
                .ativo(true)
                .build();

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(usuario)
                .build();
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }

    private TurmaEntity criarTurma(Long id, ProfessorEntity professor) {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(id);
        turma.setNome("Turma A");
        turma.setProfessor(professor);
        turma.setAtivo(true);
        return turma;
    }

    private AlunoEntity criarAluno(Long id, String nome, TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(id);
        aluno.setNome(nome);
        aluno.setAtivo(true);
        aluno.setTurma(turma);
        return aluno;
    }

    private AvaliacaoEntity criarAvaliacao(Long id, TurmaEntity turma, TipoAvaliacao tipo, Integer periodo, Integer peso) {
        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        avaliacao.setId(id);
        avaliacao.setTema("Prova 1");
        avaliacao.setData(LocalDateTime.of(2026, 5, 21, 10, 0));
        avaliacao.setPeso(peso);
        avaliacao.setTipo(tipo);
        avaliacao.setPeriodo(periodo);
        avaliacao.setTurma(turma);
        avaliacao.setNumeroChamada(1);
        avaliacao.setConcluida(false);
        return avaliacao;
    }

    private NotaEntity criarNota(Long id, AvaliacaoEntity avaliacao, AlunoEntity aluno, BigDecimal valor, Boolean naoRealizada) {
        NotaEntity nota = new NotaEntity();
        nota.setId(id);
        nota.setAvaliacao(avaliacao);
        nota.setAluno(aluno);
        nota.setValor(valor);
        nota.setNaoRealizada(naoRealizada);
        return nota;
    }
}
