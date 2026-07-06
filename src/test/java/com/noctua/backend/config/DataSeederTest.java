package com.noctua.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    // Teste 1: cria usuário admin e professor padrão quando os e-mails ainda não existem.
    @Test
    void runDeveCriarAdminEProfessorQuandoUsuariosNaoExistirem() {
        when(usuarioRepository.existsByEmail("administrador@noctua.com")).thenReturn(false);
        when(usuarioRepository.existsByEmail("professor@noctua.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("senha-hash");

        dataSeeder.run(null);

        ArgumentCaptor<AdminEntity> adminCaptor = ArgumentCaptor.forClass(AdminEntity.class);
        ArgumentCaptor<ProfessorEntity> professorCaptor = ArgumentCaptor.forClass(ProfessorEntity.class);

        verify(adminRepository).save(adminCaptor.capture());
        verify(professorRepository).save(professorCaptor.capture());

        assertEquals("Admin Noctua", adminCaptor.getValue().getUsuario().getNome());
        assertEquals("administrador@noctua.com", adminCaptor.getValue().getUsuario().getEmail());
        assertEquals("senha-hash", adminCaptor.getValue().getUsuario().getSenhaHash());
        assertEquals(true, adminCaptor.getValue().getUsuario().getAtivo());

        assertEquals("Professor Noctua", professorCaptor.getValue().getUsuario().getNome());
        assertEquals("professor@noctua.com", professorCaptor.getValue().getUsuario().getEmail());
        assertEquals("senha-hash", professorCaptor.getValue().getUsuario().getSenhaHash());
        assertEquals(true, professorCaptor.getValue().getUsuario().getAtivo());
    }

    // Teste 2: não cria usuários padrão quando os e-mails ja existem.
    @Test
    void runNaoDeveCriarUsuariosQuandoEmailsJaExistirem() {
        when(usuarioRepository.existsByEmail("administrador@noctua.com")).thenReturn(true);
        when(usuarioRepository.existsByEmail("professor@noctua.com")).thenReturn(true);

        dataSeeder.run(null);

        verify(adminRepository, never()).save(org.mockito.Mockito.any());
        verify(professorRepository, never()).save(org.mockito.Mockito.any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
