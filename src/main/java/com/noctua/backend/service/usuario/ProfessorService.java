package com.noctua.backend.service.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public void cadastrarProfessor(ProfessorRequestDTO professorRequestDTO) {

        if (professorRequestDTO.getNome() == null || professorRequestDTO.getNome().isBlank() ||
                professorRequestDTO.getEmail() == null || professorRequestDTO.getEmail().isBlank() ||
                professorRequestDTO.getSenha() == null || professorRequestDTO.getSenha().isBlank() ||
                professorRequestDTO.getCpf() == null || professorRequestDTO.getCpf().isBlank()) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }

        if (usuarioRepository.existsByEmail(professorRequestDTO.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        if (professorRepository.findByCpf(professorRequestDTO.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(criarUsuario(professorRequestDTO))
                .cpf(professorRequestDTO.getCpf())
                .build();

        professorRepository.save(professor);
    }

    private UsuarioEntity criarUsuario(ProfessorRequestDTO dto) {
        return UsuarioEntity.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .ativo(true)
                .build();
    }
}