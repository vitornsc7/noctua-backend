package com.noctua.backend.controller.twoFactor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TwoFactorVerifySetupRequestDTO {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos.")
    private String code;
}
