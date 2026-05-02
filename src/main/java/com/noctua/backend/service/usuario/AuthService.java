package com.noctua.backend.service.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noctua.backend.config.JwtUtil;
import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.dto.Login.LoginResponseDTO;
import com.noctua.backend.dto.Usuario.AuthenticatedUserResponseDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifyLoginRequestDTO;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;
import com.noctua.backend.service.twoFactor.TwoFactorService;
import com.noctua.backend.dto.Usuario.UsuarioUpdateRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TwoFactorService twoFactorService;

    public LoginResponseDTO login(LoginRequestDTO dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha inválida!");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo!");
        }

        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            return new LoginResponseDTO(null, true, "Código 2FA necessário.");
        }

        String token = jwtUtil.generateToken(dto.getEmail(), dto.isRememberMe());
        return new LoginResponseDTO(token, false, null);
    }

    public LoginResponseDTO verifyLoginTwoFactor(TwoFactorVerifyLoginRequestDTO dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        System.out.println("=== DEBUG LOGIN 2FA ===");
        System.out.println("Email recebido: " + dto.getEmail());
        System.out.println("Usuário encontrado: " + usuario.getEmail());
        System.out.println("Ativo: " + usuario.getAtivo());
        System.out.println("2FA enabled: " + usuario.getTwoFactorEnabled());
        System.out.println("TwoFactorSecret: " + usuario.getTwoFactorSecret());
        System.out.println("TwoFactorTempSecret: " + usuario.getTwoFactorTempSecret());
        System.out.println("Código recebido (texto): " + dto.getCode());

        boolean senhaCorreta = passwordEncoder.matches(dto.getSenha(), usuario.getSenhaHash());
        System.out.println("Senha confere? " + senhaCorreta);

        if (!senhaCorreta) {
            throw new IllegalArgumentException("Senha inválida!");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo!");
        }

        if (!Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            throw new IllegalArgumentException("O 2FA não está ativado para este usuário.");
        }

        if (usuario.getTwoFactorSecret() == null || usuario.getTwoFactorSecret().isBlank()) {
            throw new IllegalArgumentException("Segredo 2FA não configurado.");
        }

        int code;
        try {
            code = Integer.parseInt(dto.getCode());
            System.out.println("Código convertido para int: " + code);
        } catch (NumberFormatException e) {
            System.out.println("Falha ao converter código para int.");
            throw new IllegalArgumentException("Código 2FA inválido.");
        }

        boolean valid = twoFactorService.verifyCode(usuario.getTwoFactorSecret(), code);
        System.out.println("Código TOTP válido? " + valid);
        System.out.println("=======================");

        if (!valid) {
            throw new IllegalArgumentException("Código 2FA inválido.");
        }

        String token = jwtUtil.generateToken(dto.getEmail(), dto.isRememberMe());
        return new LoginResponseDTO(token, false, null);
    }

    public AuthenticatedUserResponseDTO buscarUsuarioAutenticado(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        return new AuthenticatedUserResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                resolverRole(email));
    }

    public AuthenticatedUserResponseDTO atualizarUsuarioAutenticado(
            String emailAtual,
            UsuarioUpdateRequestDTO dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(emailAtual)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equals(usuario.getEmail())) {

            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }

            usuario.setEmail(dto.getEmail());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        UsuarioEntity usuarioAtualizado = usuarioRepository.save(usuario);

        return new AuthenticatedUserResponseDTO(
                usuarioAtualizado.getId(),
                usuarioAtualizado.getNome(),
                usuarioAtualizado.getEmail(),
                resolverRole(usuarioAtualizado.getEmail()));
    }

    public void excluirUsuarioAutenticado(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private String resolverRole(String email) {
        if (adminRepository.findByUsuarioEmail(email).isPresent()) {
            return "ADMIN";
        }

        if (professorRepository.findByUsuarioEmail(email).isPresent()) {
            return "PROFESSOR";
        }

        throw new IllegalArgumentException("Perfil do usuário não encontrado!");
    }
}