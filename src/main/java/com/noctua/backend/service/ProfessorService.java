package com.noctua.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noctua.backend.dto.Usuario.ProfessorRequestDTO;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.repository.ProfessorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ProfessorService {
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;

    public void cadastrarProfessor(ProfessorRequestDTO professorRequestDTO) {

        if (professorRequestDTO.getNome() == null || professorRequestDTO.getNome().isBlank() ||
                professorRequestDTO.getEmail() == null || professorRequestDTO.getEmail().isBlank() ||
                professorRequestDTO.getSenha() == null || professorRequestDTO.getSenha().isBlank() ||
                professorRequestDTO.getCpf() == null || professorRequestDTO.getCpf().isBlank()) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }

        if (professorRepository.findByEmail(professorRequestDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        if (professorRepository.findByCpf(professorRequestDTO.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        ProfessorEntity professor = new ProfessorEntity();
        professor.setNome(professorRequestDTO.getNome());
        professor.setEmail(professorRequestDTO.getEmail());
        professor.setCpf(professorRequestDTO.getCpf());
        professor.setSenhaHash(passwordEncoder.encode(professorRequestDTO.getSenha()));
        professor.setAtivo(true);

        professorRepository.save(professor);
    }
}
