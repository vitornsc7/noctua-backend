package com.noctua.backend.controller.usuario;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.service.usuario.ProfessorService;

@ExtendWith(MockitoExtension.class)
class ProfessorControllerTest {

    @Mock
    private ProfessorService professorService;

    @InjectMocks
    private ProfessorController professorController;

    // Teste 1: endpoint POST /professores cadastra professor e retorna 201.
    @Test
    void cadastrarDeveRetornarCreatedQuandoProfessorForCadastrado() {
        ProfessorRequestDTO request = criarProfessorRequest();

        ResponseEntity<String> response = professorController.cadastrar(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Professor cadastrado com sucesso.", response.getBody());
        verify(professorService).cadastrarProfessor(request);
    }

    // Teste 2: endpoint POST /professores retorna 400 quando a service valida erro.
    @Test
    void cadastrarDeveRetornarBadRequestQuandoServiceLancarIllegalArgumentException() {
        ProfessorRequestDTO request = criarProfessorRequest();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("E-mail já cadastrado."))
                .when(professorService)
                .cadastrarProfessor(request);

        ResponseEntity<String> response = professorController.cadastrar(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("E-mail já cadastrado.", response.getBody());
    }

    // Teste 3: endpoint POST /professores retorna 500 quando acontece erro inesperado.
    @Test
    void cadastrarDeveRetornarInternalServerErrorQuandoServiceLancarErroInesperado() {
        ProfessorRequestDTO request = criarProfessorRequest();
        org.mockito.Mockito.doThrow(new RuntimeException("falha no banco"))
                .when(professorService)
                .cadastrarProfessor(request);

        ResponseEntity<String> response = professorController.cadastrar(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno ao cadastrar professor: falha no banco", response.getBody());
    }

    private ProfessorRequestDTO criarProfessorRequest() {
        ProfessorRequestDTO request = new ProfessorRequestDTO();
        request.setNome("Professor");
        request.setEmail("prof@email.com");
        request.setSenha("123456");
        request.setAtivo(true);
        request.setDataExpiracao(LocalDateTime.of(2026, 12, 31, 23, 59));
        return request;
    }
}
