package com.noctua.backend.service.usuario;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfessorService professorService;

    @Test
    void cadastrarProfessorDeveLancarErroQuandoCamposObrigatoriosEstiveremVazios() {
        ProfessorRequestDTO request = new ProfessorRequestDTO();
        request.setNome("Professor Teste");
        request.setEmail("");
        request.setSenha("123456");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> professorService.cadastrarProfessor(request));

        assertEquals("Todos os campos são obrigatórios.", exception.getMessage());
        verifyNoInteractions(usuarioRepository, professorRepository, passwordEncoder);
    }

    @Test
    void cadastrarProfessorDeveLancarErroQuandoEmailJaEstiverCadastrado() {
        ProfessorRequestDTO request = criarRequest();

        when(usuarioRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> professorService.cadastrarProfessor(request));

        assertEquals("E-mail já cadastrado.", exception.getMessage());
        verify(passwordEncoder, never()).encode(org.mockito.Mockito.anyString());
        verify(professorRepository, never()).save(org.mockito.Mockito.any());
    }

    @Test
    void cadastrarProfessorDeveSalvarUsuarioComSenhaCriptografadaESalvarProfessor() {
        ProfessorRequestDTO request = criarRequest();

        when(usuarioRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getSenha()))
                .thenReturn("senha-hash");

        when(usuarioRepository.save(org.mockito.Mockito.any(UsuarioEntity.class)))
                .thenAnswer(invocation -> {
                    UsuarioEntity usuario = invocation.getArgument(0);
                    usuario.setId(1L);
                    return usuario;
                });

        professorService.cadastrarProfessor(request);

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        UsuarioEntity usuarioSalvo = usuarioCaptor.getValue();
        assertEquals(request.getNome(), usuarioSalvo.getNome());
        assertEquals(request.getEmail(), usuarioSalvo.getEmail());
        assertEquals("senha-hash", usuarioSalvo.getSenhaHash());
        assertEquals(true, usuarioSalvo.getAtivo());

        ArgumentCaptor<ProfessorEntity> professorCaptor = ArgumentCaptor.forClass(ProfessorEntity.class);
        verify(professorRepository).save(professorCaptor.capture());

        ProfessorEntity professorSalvo = professorCaptor.getValue();
        assertEquals(usuarioSalvo, professorSalvo.getUsuario());
    }

    private ProfessorRequestDTO criarRequest() {
        ProfessorRequestDTO request = new ProfessorRequestDTO();
        request.setNome("Professor Teste");
        request.setEmail("professor@email.com");
        request.setSenha("123456");
        return request;
    }
}
