package com.noctua.backend.dto.Login;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private boolean requiresTwoFactor;
    private String message;
}
