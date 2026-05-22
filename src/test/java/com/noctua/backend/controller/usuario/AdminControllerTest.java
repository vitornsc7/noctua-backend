package com.noctua.backend.controller.usuario;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.noctua.backend.dto.AiRequestLog.AiRequestLogDTO;
import com.noctua.backend.dto.Usuario.AdminMonitoringProfessorDTO;
import com.noctua.backend.dto.Usuario.AdminMonitoringResponseDTO;
import com.noctua.backend.dto.Usuario.AdminRequestDTO;
import com.noctua.backend.service.usuario.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    // Teste 1: endpoint /admins/monitoramento retorna dados de monitoramento do admin autenticado.
    @Test
    void monitoramentoDeveRetornarDadosDoAdminAutenticado() {
        AdminMonitoringResponseDTO serviceResponse = new AdminMonitoringResponseDTO(
                2,
                1,
                List.of(new AdminMonitoringProfessorDTO(
                        10L,
                        "Professor",
                        "prof@email.com",
                        true,
                        120L)));

        when(authentication.getName()).thenReturn("admin@email.com");
        when(adminService.buscarMonitoramento("admin@email.com")).thenReturn(serviceResponse);

        ResponseEntity<AdminMonitoringResponseDTO> response = adminController.monitoramento(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(adminService).buscarMonitoramento("admin@email.com");
    }

    // Teste 2: endpoint /admins/logs retorna logs filtrando por professor quando professorId é informado.
    @Test
    void logsDeveRetornarLogsComFiltroDeProfessor() {
        List<AiRequestLogDTO> serviceResponse = List.of(new AiRequestLogDTO(
                1L,
                10L,
                "Professor",
                LocalDateTime.of(2026, 5, 22, 10, 0),
                42));

        when(authentication.getName()).thenReturn("admin@email.com");
        when(adminService.buscarLogs("admin@email.com", 10L)).thenReturn(serviceResponse);

        ResponseEntity<List<AiRequestLogDTO>> response = adminController.logs(authentication, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(adminService).buscarLogs("admin@email.com", 10L);
    }

    // Teste 3: endpoint POST /admins cadastra admin e retorna 201.
    @Test
    void cadastrarDeveRetornarCreatedQuandoAdminForCadastrado() {
        AdminRequestDTO request = criarAdminRequest();

        ResponseEntity<String> response = adminController.cadastrar(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Admin cadastrado com sucesso!", response.getBody());
        verify(adminService).cadastrarAdmin(request);
    }

    // Teste 4: endpoint POST /admins retorna 400 quando a service valida erro.
    @Test
    void cadastrarDeveRetornarBadRequestQuandoServiceLancarIllegalArgumentException() {
        AdminRequestDTO request = criarAdminRequest();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("E-mail já cadastrado."))
                .when(adminService)
                .cadastrarAdmin(request);

        ResponseEntity<String> response = adminController.cadastrar(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("E-mail já cadastrado.", response.getBody());
    }

    // Teste 5: endpoint POST /admins retorna 500 quando acontece erro inesperado.
    @Test
    void cadastrarDeveRetornarInternalServerErrorQuandoServiceLancarErroInesperado() {
        AdminRequestDTO request = criarAdminRequest();
        org.mockito.Mockito.doThrow(new RuntimeException("erro"))
                .when(adminService)
                .cadastrarAdmin(request);

        ResponseEntity<String> response = adminController.cadastrar(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno ao cadastrar admin!", response.getBody());
    }

    private AdminRequestDTO criarAdminRequest() {
        AdminRequestDTO request = new AdminRequestDTO();
        request.setNome("Admin");
        request.setEmail("admin@email.com");
        request.setSenha("123456");
        return request;
    }
}
