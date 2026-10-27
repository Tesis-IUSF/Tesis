package com.tesis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank
        @Email
        @Size(max = 150)
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String token;
        private Integer usuarioId;
        private String nombreUsuario;
        private String email;
        private String rolNombre;
        private String tipoUsuario;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenValidationResponse {
        private boolean valid;
        private Integer usuarioId;
        private String nombreUsuario;
        private String email;
    }
}