package com.noctua.backend.dto.Usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Token é obrigatório.")
    private String token;

    @NotBlank(message = "Nova senha é obrigatória.")
    private String novaSenha;

    @NotBlank(message = "Confirmação de senha é obrigatória.")
    private String confirmacaoSenha;
}
