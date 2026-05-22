package com.noctua.backend.service.turma;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import com.noctua.backend.dto.Aluno.AlunoRequestDTO;
import com.noctua.backend.dto.Aluno.AlunoResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @InjectMocks
    private AlunoService alunoService;

    // Teste 1: cria aluno ativo por padrão quando o request não informa ativo.
    @Test
    void criarDeveSalvarAlunoAtivoPorPadraoQuandoAtivoNaoForInformado() {
        TurmaEntity turma = criarTurma(10L);
        AlunoRequestDTO request = new AlunoRequestDTO("Ana", "Observacao", null, 10L);

        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(alunoRepository.save(org.mockito.Mockito.any(AlunoEntity.class))).thenAnswer(invocation -> {
            AlunoEntity aluno = invocation.getArgument(0);
            aluno.setId(1L);
            return aluno;
        });

        AlunoResponseDTO response = alunoService.criar(10L, request);

        assertEquals(1L, response.getId());
        assertEquals("Ana", response.getNome());
        assertEquals("Observacao", response.getObservacao());
        assertEquals(true, response.getAtivo());
        assertEquals(10L, response.getTurmaId());

        ArgumentCaptor<AlunoEntity> alunoCaptor = ArgumentCaptor.forClass(AlunoEntity.class);
        verify(alunoRepository).save(alunoCaptor.capture());
        assertEquals(true, alunoCaptor.getValue().getAtivo());
        assertEquals(turma, alunoCaptor.getValue().getTurma());
    }

    // Teste 2: não cria aluno quando a turma não existe.
    @Test
    void criarDeveLancarErroQuandoTurmaNaoForEncontrada() {
        when(turmaRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> alunoService.criar(10L, new AlunoRequestDTO("Ana", null, true, 10L)));

        assertEquals("Turma não encontrada", exception.getMessage());
        verify(alunoRepository, never()).save(org.mockito.Mockito.any());
    }

    // Teste 3: lista todos os alunos da turma quando filtro ativo não e informado.
    @Test
    void listarPorTurmaDeveBuscarTodosQuandoAtivoForNulo() {
        TurmaEntity turma = criarTurma(10L);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);

        when(alunoRepository.findByTurmaId(10L)).thenReturn(List.of(aluno));

        List<AlunoResponseDTO> response = alunoService.listarPorTurma(10L, null);

        assertEquals(1, response.size());
        assertEquals("Ana", response.get(0).getNome());
        verify(alunoRepository).findByTurmaId(10L);
    }

    // Teste 4: lista alunos filtrando por ativo quando o filtro e informado.
    @Test
    void listarPorTurmaDeveFiltrarPorAtivoQuandoAtivoForInformado() {
        TurmaEntity turma = criarTurma(10L);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);

        when(alunoRepository.findByTurmaIdAndAtivo(10L, true)).thenReturn(List.of(aluno));

        List<AlunoResponseDTO> response = alunoService.listarPorTurma(10L, true);

        assertEquals(1, response.size());
        assertEquals(true, response.get(0).getAtivo());
        verify(alunoRepository).findByTurmaIdAndAtivo(10L, true);
    }

    // Teste 5: lista alunos paginado sem filtro ativo quando o filtro é nulo.
    @Test
    void listarPorTurmaPaginadoDeveBuscarPaginaSemFiltroAtivo() {
        TurmaEntity turma = criarTurma(10L);
        Pageable pageable = PageRequest.of(0, 10);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);

        when(alunoRepository.findByTurmaId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(aluno), pageable, 1));

        Page<AlunoResponseDTO> response = alunoService.listarPorTurmaPaginado(10L, null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Ana", response.getContent().get(0).getNome());
        verify(alunoRepository).findByTurmaId(10L, pageable);
    }

    // Teste 6: busca aluno por id e converte para response DTO.
    @Test
    void buscarPorIdDeveRetornarAlunoQuandoEncontrado() {
        TurmaEntity turma = criarTurma(10L);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);

        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));

        AlunoResponseDTO response = alunoService.buscarPorId(1L);

        assertEquals(1L, response.getId());
        assertEquals("Ana", response.getNome());
        assertEquals(10L, response.getTurmaId());
    }

    // Teste 7: atualiza dados e ativo quando ativo vem preenchido no request.
    @Test
    void atualizarDeveAlterarDadosDoAluno() {
        TurmaEntity turma = criarTurma(10L);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);
        AlunoRequestDTO request = new AlunoRequestDTO("Ana Maria", "Nova observacao", false, 10L);

        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(alunoRepository.save(aluno)).thenReturn(aluno);

        AlunoResponseDTO response = alunoService.atualizar(1L, request);

        assertEquals("Ana Maria", response.getNome());
        assertEquals("Nova observacao", response.getObservacao());
        assertEquals(false, response.getAtivo());
        verify(alunoRepository).save(aluno);
    }

    // Teste 8: atualizar preserva ativo quando request não informa ativo.
    @Test
    void atualizarDevePreservarAtivoQuandoAtivoNaoForInformado() {
        TurmaEntity turma = criarTurma(10L);
        AlunoEntity aluno = criarAluno(1L, "Ana", true, turma);
        AlunoRequestDTO request = new AlunoRequestDTO("Ana Maria", null, null, 10L);

        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(alunoRepository.save(aluno)).thenReturn(aluno);

        AlunoResponseDTO response = alunoService.atualizar(1L, request);

        assertEquals(true, response.getAtivo());
    }

    // Teste 9: deletar remove aluno quando ele existe.
    @Test
    void deletarDeveRemoverAlunoQuandoExistir() {
        when(alunoRepository.existsById(1L)).thenReturn(true);

        alunoService.deletar(1L);

        verify(alunoRepository).deleteById(1L);
    }

    // Teste 10: deletar lança erro quando aluno não existe.
    @Test
    void deletarDeveLancarErroQuandoAlunoNaoExistir() {
        when(alunoRepository.existsById(1L)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> alunoService.deletar(1L));

        assertEquals("Aluno não encontrado", exception.getMessage());
        verify(alunoRepository, never()).deleteById(1L);
    }

    private TurmaEntity criarTurma(Long id) {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(id);
        turma.setNome("Turma A");
        turma.setAtivo(true);
        return turma;
    }

    private AlunoEntity criarAluno(Long id, String nome, Boolean ativo, TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(id);
        aluno.setNome(nome);
        aluno.setObservacao("Observacao");
        aluno.setAtivo(ativo);
        aluno.setTurma(turma);
        return aluno;
    }
}
