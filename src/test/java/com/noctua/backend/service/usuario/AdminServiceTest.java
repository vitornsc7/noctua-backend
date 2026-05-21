package com.noctua.backend.service.usuario;

import java.time.LocalDateTime;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.AiRequestLog.AiRequestLogDTO;
import com.noctua.backend.dto.Usuario.AdminMonitoringResponseDTO;
import com.noctua.backend.dto.Usuario.AdminRequestDTO;
import com.noctua.backend.entity.AiRequestLog.AiRequestLogEntity;
import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.AiRequestLogRepository;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void cadastrarAdminDeveLancarErroQuandoCamposObrigatoriosEstiveremVazios() {
        AdminRequestDTO request = new AdminRequestDTO();
        request.setNome("Admin Teste");
        request.setEmail("");
        request.setSenha("123456");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.cadastrarAdmin(request));

        assertEquals("Todos os campos são obrigatórios.", exception.getMessage());
        verifyNoInteractions(usuarioRepository, adminRepository, passwordEncoder);
    }

    @Test
    void cadastrarAdminDeveLancarErroQuandoEmailJaEstiverCadastrado() {
        AdminRequestDTO request = criarAdminRequest();

        when(usuarioRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.cadastrarAdmin(request));

        assertEquals("E-mail já cadastrado.", exception.getMessage());
        verify(passwordEncoder, never()).encode(org.mockito.Mockito.anyString());
        verify(adminRepository, never()).save(org.mockito.Mockito.any());
    }

    @Test
    void cadastrarAdminDeveSalvarAdminComUsuarioAtivoESenhaCriptografada() {
        AdminRequestDTO request = criarAdminRequest();

        when(usuarioRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getSenha()))
                .thenReturn("senha-hash");

        adminService.cadastrarAdmin(request);

        ArgumentCaptor<AdminEntity> adminCaptor = ArgumentCaptor.forClass(AdminEntity.class);
        verify(adminRepository).save(adminCaptor.capture());

        UsuarioEntity usuario = adminCaptor.getValue().getUsuario();
        assertEquals(request.getNome(), usuario.getNome());
        assertEquals(request.getEmail(), usuario.getEmail());
        assertEquals("senha-hash", usuario.getSenhaHash());
        assertEquals(true, usuario.getAtivo());
    }

    @Test
    void buscarMonitoramentoDeveLancarForbiddenQuandoUsuarioNaoForAdmin() {
        when(adminRepository.findByUsuarioEmail("nao-admin@email.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.buscarMonitoramento("nao-admin@email.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(professorRepository, aiRequestLogRepository);
    }

    @Test
    void buscarMonitoramentoDeveRetornarTotaisEProfessoresComTokens() {
        when(adminRepository.findByUsuarioEmail("admin@email.com"))
                .thenReturn(Optional.of(criarAdmin()));

        ProfessorEntity professor = criarProfessor(10L, "Professora Um", "prof1@email.com", true);

        List<Object[]> tokensPorProfessor = java.util.Collections.singletonList(new Object[] { 10L, 120L });
        when(aiRequestLogRepository.sumTokensPorProfessor())
                .thenReturn(tokensPorProfessor);

        when(professorRepository.findAllByOrderByUsuarioNomeAsc())
                .thenReturn(List.of(professor));

        when(professorRepository.countByUsuarioAtivoTrue())
                .thenReturn(1L);

        AdminMonitoringResponseDTO response = adminService.buscarMonitoramento("admin@email.com");

        assertEquals(1L, response.getTotalProfessores());
        assertEquals(1L, response.getProfessoresAtivos());
        assertEquals(1, response.getProfessores().size());
        assertEquals("Professora Um", response.getProfessores().get(0).getNome());
        assertEquals("prof1@email.com", response.getProfessores().get(0).getEmail());
        assertEquals(true, response.getProfessores().get(0).getAtivo());
        assertEquals(120L, response.getProfessores().get(0).getTotalTokens());
    }

    @Test
    void buscarLogsDeveRetornarLogsDeTodosOsProfessoresQuandoProfessorIdForNulo() {
        when(adminRepository.findByUsuarioEmail("admin@email.com"))
                .thenReturn(Optional.of(criarAdmin()));

        LocalDateTime dataRequest = LocalDateTime.now();
        ProfessorEntity professor = criarProfessor(10L, "Professora Um", "prof1@email.com", true);
        AiRequestLogEntity log = new AiRequestLogEntity(5L, professor, dataRequest, 42);

        when(aiRequestLogRepository.findAllByOrderByDataRequestDesc())
                .thenReturn(List.of(log));

        List<AiRequestLogDTO> response = adminService.buscarLogs("admin@email.com", null);

        assertEquals(1, response.size());
        assertEquals(5L, response.get(0).getId());
        assertEquals(10L, response.get(0).getProfessorId());
        assertEquals("Professora Um", response.get(0).getProfessorNome());
        assertEquals(dataRequest, response.get(0).getDataRequest());
        assertEquals(42, response.get(0).getTokensUsados());
    }

    @Test
    void buscarLogsDeveFiltrarPorProfessorQuandoProfessorIdForInformado() {
        when(adminRepository.findByUsuarioEmail("admin@email.com"))
                .thenReturn(Optional.of(criarAdmin()));

        when(aiRequestLogRepository.findByProfessorIdOrderByDataRequestDesc(10L))
                .thenReturn(List.of());

        List<AiRequestLogDTO> response = adminService.buscarLogs("admin@email.com", 10L);

        assertEquals(0, response.size());
        verify(aiRequestLogRepository).findByProfessorIdOrderByDataRequestDesc(10L);
        verify(aiRequestLogRepository, never()).findAllByOrderByDataRequestDesc();
    }

    private AdminRequestDTO criarAdminRequest() {
        AdminRequestDTO request = new AdminRequestDTO();
        request.setNome("Admin Teste");
        request.setEmail("admin@email.com");
        request.setSenha("123456");
        return request;
    }

    private AdminEntity criarAdmin() {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(1L)
                .nome("Admin Teste")
                .email("admin@email.com")
                .senhaHash("senha-hash")
                .ativo(true)
                .build();

        return AdminEntity.builder()
                .usuario(usuario)
                .build();
    }

    private ProfessorEntity criarProfessor(Long id, String nome, String email, Boolean ativo) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(id)
                .nome(nome)
                .email(email)
                .senhaHash("senha-hash")
                .ativo(ativo)
                .build();

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(usuario)
                .build();
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }
}
