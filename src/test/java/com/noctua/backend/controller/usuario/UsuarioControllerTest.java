package com.noctua.backend.controller.usuario;

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
class UsuarioControllerTest {

    @Mock
    private ProfessorService professorService;

    @InjectMocks
    private UsuarioController usuarioController;

    // Teste 1: endpoint /auth/register cadastra professor e retorna mensagem de sucesso.
    @Test
    void registerDeveCadastrarProfessorERetornarMensagemDeSucesso() {
        ProfessorRequestDTO request = criarProfessorRequest();

        ResponseEntity<String> response = usuarioController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Professor cadastrado com sucesso!", response.getBody());
        verify(professorService).cadastrarProfessor(request);
    }

    private ProfessorRequestDTO criarProfessorRequest() {
        ProfessorRequestDTO request = new ProfessorRequestDTO();
        request.setNome("Professor");
        request.setEmail("prof@email.com");
        request.setSenha("123456");
        request.setAtivo(true);
        return request;
    }
}
