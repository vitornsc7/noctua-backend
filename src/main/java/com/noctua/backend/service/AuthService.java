package com.noctua.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.noctua.backend.config.JwtUtil;
import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String login(LoginRequestDTO dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado!"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha inválida!");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo!");
        }

        return jwtUtil.generateToken(dto.getEmail(), dto.isRememberMe());
    }
}