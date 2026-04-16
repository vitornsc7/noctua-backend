package com.noctua.backend.dto.twoFactor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TwoFactorSetupResponseDTO {
    
    private String secret;
    private String otpauthUrl;
}
