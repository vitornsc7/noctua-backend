package com.noctua.backend.dto.Login;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class LoginRequestDTO {

    private String email;
    private String senha;
    private boolean rememberMe;

}
