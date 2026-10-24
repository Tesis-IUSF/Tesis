package com.tesis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class InitialSetupDTO {

    private InitialSetupDTO() {
    }

    public record StatusResponseDTO(
            boolean initialSetupRequired,
            boolean setupTokenConfigured) {
    }

    public record InitialAdminRequestDTO(
            @NotBlank
            @Size(min = 3, max = 100)
            @Pattern(regexp = "[A-Za-z0-9._-]+",
                    message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
            String nombreUsuario,
            @NotBlank
            @Email
            @Size(max = 150)
            String email,
            @NotBlank
            @Size(min = 12, max = 72,
                    message = "La contraseña debe tener entre 12 y 72 caracteres")
            String password) {
    }

    public record InitialAdminResponseDTO(
            Integer usuarioId,
            String nombreUsuario,
            String email,
            String rol) {
    }
}
