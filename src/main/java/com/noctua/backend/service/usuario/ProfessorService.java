package com.noctua.backend.service.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Usuario.LimitesRequestDTO;
import com.noctua.backend.dto.Usuario.LimitesResponseDTO;
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

    @Transactional
    public void cadastrarProfessor(ProfessorRequestDTO professorRequestDTO) {

        if (professorRequestDTO.getNome() == null || professorRequestDTO.getNome().isBlank() ||
                professorRequestDTO.getEmail() == null || professorRequestDTO.getEmail().isBlank() ||
                professorRequestDTO.getSenha() == null || professorRequestDTO.getSenha().isBlank()) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }

        if (usuarioRepository.existsByEmail(professorRequestDTO.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        UsuarioEntity usuario = usuarioRepository.save(criarUsuario(professorRequestDTO));

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(usuario)
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

    public LimitesResponseDTO getLimites(String email) {
        ProfessorEntity professor = professorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));
        return LimitesResponseDTO.builder()
                .atencaoFim(professor.getFrequenciaAtencaoFim() != null ? professor.getFrequenciaAtencaoFim() : 79.9)
                .regularFim(professor.getFrequenciaRegularFim() != null ? professor.getFrequenciaRegularFim() : 89.9)
                .pontosAcima(professor.getDesempenhoAcima() != null ? professor.getDesempenhoAcima() : 2.0)
                .pontosAbaixo(professor.getDesempenhoAbaixo() != null ? professor.getDesempenhoAbaixo() : 1.0)
                .build();
    }

    @Transactional
    public LimitesResponseDTO atualizarLimites(String email, LimitesRequestDTO request) {
        ProfessorEntity professor = professorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));

        if (request.getAtencaoFim() == null || request.getRegularFim() == null
                || request.getPontosAcima() == null || request.getPontosAbaixo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos os campos são obrigatórios.");
        }

        professor.setFrequenciaAtencaoFim(request.getAtencaoFim());
        professor.setFrequenciaRegularFim(request.getRegularFim());
        professor.setDesempenhoAcima(request.getPontosAcima());
        professor.setDesempenhoAbaixo(request.getPontosAbaixo());

        professorRepository.save(professor);

        return LimitesResponseDTO.builder()
                .atencaoFim(professor.getFrequenciaAtencaoFim())
                .regularFim(professor.getFrequenciaRegularFim())
                .pontosAcima(professor.getDesempenhoAcima())
                .pontosAbaixo(professor.getDesempenhoAbaixo())
                .build();
    }