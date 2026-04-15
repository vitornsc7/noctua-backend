package com.noctua.backend.service.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Usuario.AdminRequestDTO;
import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public void cadastrarAdmin(AdminRequestDTO adminRequestDTO) {

        if (adminRequestDTO.getNome() == null || adminRequestDTO.getNome().isBlank() ||
                adminRequestDTO.getEmail() == null || adminRequestDTO.getEmail().isBlank() ||
                adminRequestDTO.getSenha() == null || adminRequestDTO.getSenha().isBlank()) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }

        if (usuarioRepository.existsByEmail(adminRequestDTO.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        AdminEntity admin = AdminEntity.builder()
                .usuario(criarUsuario(adminRequestDTO))
                .build();

        adminRepository.save(admin);
    }

    private UsuarioEntity criarUsuario(AdminRequestDTO dto) {
        return UsuarioEntity.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .ativo(true)
                .build();
    }
}