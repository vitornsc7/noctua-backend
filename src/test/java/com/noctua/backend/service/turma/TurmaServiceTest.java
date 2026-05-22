package com.noctua.backend.service.turma;

import java.time.LocalDate;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Turma.TurmaFiltrosDTO;
import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.TurmaRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @InjectMocks
    private TurmaService turmaService;

    // Teste 1: cria turma ativa vinculada ao professor autenticado.
    @Test
    void criarDeveSalvarTurmaAtivaParaProfessorAutenticado() {
        ProfessorEntity professor = criarProfessor(1L);
        TurmaRequestDTO request = criarRequest("Turma A");

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.save(any(TurmaEntity.class))).thenAnswer(invocation -> {
            TurmaEntity turma = invocation.getArgument(0);
            turma.setId(10L);
            return turma;
        });

        TurmaResponseDTO response = turmaService.criar("prof@email.com", request);

        assertEquals(10L, response.getId());
        assertEquals("Turma A", response.getNome());
        assertEquals(Turno.MATUTINO, response.getTurno());
        assertEquals(0, response.getAlunosCount());

        ArgumentCaptor<TurmaEntity> turmaCaptor = ArgumentCaptor.forClass(TurmaEntity.class);
        verify(turmaRepository).save(turmaCaptor.capture());
        assertEquals(true, turmaCaptor.getValue().getAtivo());
        assertEquals(professor, turmaCaptor.getValue().getProfessor());
    }

    // Teste 2: não cria turma quando o professor autenticado não existe.
    @Test
    void criarDeveLancarForbiddenQuandoProfessorNaoForEncontrado() {
        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> turmaService.criar("prof@email.com", criarRequest("Turma A")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(turmaRepository, never()).save(any());
    }

    // Teste 3: lista turmas ativas do professor usando filtros e pageable.
    @Test
    void listarDeveRetornarPaginaDeTurmasDoProfessorComFiltros() {
        ProfessorEntity professor = criarProfessor(1L);
        Pageable pageable = PageRequest.of(0, 10);
        TurmaEntity turma = criarTurma(10L, professor);

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findAll(org.mockito.ArgumentMatchers.<Specification<TurmaEntity>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(turma), pageable, 1));

        Page<TurmaResponseDTO> response = turmaService.listar(
                "prof@email.com",
                pageable,
                "matutino",
                "2026",
                "IFSP",
                "Matematica");

        assertEquals(1, response.getTotalElements());
        assertEquals("Turma A", response.getContent().get(0).getNome());
        verify(turmaRepository).findAll(org.mockito.ArgumentMatchers.<Specification<TurmaEntity>>any(), eq(pageable));
    }

    // Teste 4: busca filtros disponíveis para as turmas do professor.
    @Test
    void buscarFiltrosDeveRetornarAnosInstituicoesEDisciplinasDoProfessor() {
        ProfessorEntity professor = criarProfessor(1L);

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findDistinctAnosByProfessorId(1L)).thenReturn(List.of(2026, 2025));
        when(turmaRepository.findDistinctInstituicoesByProfessorId(1L)).thenReturn(List.of("IFSP"));
        when(turmaRepository.findDistinctDisciplinasByProfessorId(1L)).thenReturn(List.of("Matematica"));

        TurmaFiltrosDTO response = turmaService.buscarFiltros("prof@email.com");

        assertEquals(List.of(2026, 2025), response.getAnos());
        assertEquals(List.of("IFSP"), response.getInstituicoes());
        assertEquals(List.of("Matematica"), response.getDisciplinas());
    }

    // Teste 5: busca turma por id e converte alunos vinculados para response DTO.
    @Test
    void buscarPorIdDeveRetornarTurmaComAlunosQuandoPertencerAoProfessor() {
        ProfessorEntity professor = criarProfessor(1L);
        TurmaEntity turma = criarTurma(10L, professor);
        turma.setAlunos(List.of(criarAluno(100L, "Ana", turma)));

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findByIdAndProfessorIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(turma));

        TurmaResponseDTO response = turmaService.buscarPorId("prof@email.com", 10L);

        assertEquals(10L, response.getId());
        assertEquals(1, response.getAlunosCount());
        assertEquals("Ana", response.getAlunos().get(0).getNome());
        assertEquals(10L, response.getAlunos().get(0).getTurmaId());
    }

    // Teste 6: atualizar altera os dados da turma encontrada para o professor.
    @Test
    void atualizarDeveAlterarDadosDaTurma() {
        ProfessorEntity professor = criarProfessor(1L);
        TurmaEntity turma = criarTurma(10L, professor);
        TurmaRequestDTO request = criarRequest("Turma Atualizada");
        request.setDisciplina("Portugues");
        request.setInstituicao("Etec");

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findByIdAndProfessorIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(turma));
        when(turmaRepository.save(turma)).thenReturn(turma);

        TurmaResponseDTO response = turmaService.atualizar("prof@email.com", 10L, request);

        assertEquals("Turma Atualizada", response.getNome());
        assertEquals("Portugues", response.getDisciplina());
        assertEquals("Etec", response.getInstituicao());
        verify(turmaRepository).save(turma);
    }

    // Teste 7: deletar faz exclusão lógica marcando a turma como inativa.
    @Test
    void deletarDeveMarcarTurmaComoInativaQuandoEncontrada() {
        ProfessorEntity professor = criarProfessor(1L);
        TurmaEntity turma = criarTurma(10L, professor);

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findByIdAndProfessorIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(turma));

        turmaService.deletar("prof@email.com", 10L);

        assertFalse(turma.getAtivo());
        verify(turmaRepository).save(turma);
    }

    // Teste 8: deletar retorna not found quando a turma não pertence ao professor ou não está ativa.
    @Test
    void deletarDeveLancarNotFoundQuandoTurmaNaoForEncontrada() {
        ProfessorEntity professor = criarProfessor(1L);

        when(professorRepository.findByUsuarioEmail("prof@email.com")).thenReturn(Optional.of(professor));
        when(turmaRepository.findByIdAndProfessorIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> turmaService.deletar("prof@email.com", 10L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(turmaRepository, never()).save(any());
    }

    private TurmaRequestDTO criarRequest(String nome) {
        return new TurmaRequestDTO(
                nome,
                LocalDate.of(2026, 1, 1),
                4,
                20,
                Turno.MATUTINO,
                "Matematica",
                6.0,
                "IFSP");
    }

    private ProfessorEntity criarProfessor(Long id) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(id)
                .nome("Professor")
                .email("prof@email.com")
                .senhaHash("hash")
                .ativo(true)
                .build();

        return ProfessorEntity.builder()
                .id(id)
                .usuario(usuario)
                .build();
    }

    private TurmaEntity criarTurma(Long id, ProfessorEntity professor) {
        TurmaEntity turma = new TurmaEntity();
        turma.setId(id);
        turma.setNome("Turma A");
        turma.setAnoLetivo(LocalDate.of(2026, 1, 1));
        turma.setQtdePeriodos(4);
        turma.setQtdeAulasPrevistasPeriodo(20);
        turma.setTurno(Turno.MATUTINO);
        turma.setDisciplina("Matematica");
        turma.setMediaMinima(6.0);
        turma.setInstituicao("IFSP");
        turma.setAtivo(true);
        turma.setProfessor(professor);
        return turma;
    }

    private AlunoEntity criarAluno(Long id, String nome, TurmaEntity turma) {
        AlunoEntity aluno = new AlunoEntity();
        aluno.setId(id);
        aluno.setNome(nome);
        aluno.setObservacao("Observacao");
        aluno.setAtivo(true);
        aluno.setTurma(turma);
        return aluno;
    }
}
