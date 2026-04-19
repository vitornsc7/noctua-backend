package com.noctua.backend.service.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noctua.backend.dto.Usuario.AdminRequestDTO;
import com.noctua.backend.dto.Usuario.AdminMonitoringProfessorDTO;
import com.noctua.backend.dto.Usuario.AdminMonitoringResponseDTO;
import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final ProfessorRepository professorRepository;
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

    public AdminMonitoringResponseDTO buscarMonitoramento(String emailAdmin) {
        garantirAdmin(emailAdmin);

        List<AdminMonitoringProfessorDTO> professores = professorRepository.findAllByOrderByUsuarioNomeAsc().stream()
                .map(this::toMonitoringProfessorDTO)
                .toList();

        return new AdminMonitoringResponseDTO(
                professores.size(),
                professorRepository.countByUsuarioAtivoTrue(),
                professores);
    }

    private UsuarioEntity criarUsuario(AdminRequestDTO dto) {
        return UsuarioEntity.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .ativo(true)
                .build();
    }

    private void garantirAdmin(String emailAdmin) {
        adminRepository.findByUsuarioEmail(emailAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador não encontrado"));
    }

    private AdminMonitoringProfessorDTO toMonitoringProfessorDTO(ProfessorEntity professor) {
        return new AdminMonitoringProfessorDTO(
                professor.getId(),
                professor.getUsuario().getNome(),
                professor.getUsuario().getEmail(),
                professor.getUsuario().getAtivo(),
                professor.getDataExpiracao());
    }
}