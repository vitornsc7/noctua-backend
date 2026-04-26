package com.noctua.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedProfessor();
    }

    private void seedAdmin() {
        if (usuarioRepository.existsByEmail("admin@noctua.com"))
            return;

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Administrador")
                .email("admin@noctua.com")
                .senhaHash(passwordEncoder.encode("123"))
                .ativo(true)
                .build();

        AdminEntity admin = AdminEntity.builder()
                .usuario(usuario)
                .build();

        adminRepository.save(admin);
    }

    private void seedProfessor() {
        if (usuarioRepository.existsByEmail("prof@noctua.com"))
            return;

        if (professorRepository.existsByCpf("00000000000"))
            return;

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Professor Padrão")
                .email("prof@noctua.com")
                .senhaHash(passwordEncoder.encode("123"))
                .ativo(true)
                .build();

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(usuario)
                .cpf("00000000000")
                .build();

        professorRepository.save(professor);
    }
}
