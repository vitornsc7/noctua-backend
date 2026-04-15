package com.noctua.backend.dto.twoFactor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TwoFactorVerifyLoginRequestDTO {
    private String email;
    private String senha;
    private String code;
    private boolean rememberMe;
}
