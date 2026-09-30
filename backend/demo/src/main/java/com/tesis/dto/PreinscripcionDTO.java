package com.tesis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class PreinscripcionDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreinscripcionRequestDTO {
        @Valid
        @NotNull
        private EstudianteData estudiante;

        @Valid
        @NotNull
        private RepresentanteData representante;

        @NotNull
        private Integer seccionId;

        @NotNull
        private Short anioEscolar;

        private Boolean relacionPrincipal = true;
        private Boolean autorizadoRetirar = true;
        private Boolean recibeComunicados = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstudianteData {
        @NotBlank
        @Size(max = 20)
        private String cedula;

        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotBlank
        @Size(max = 100)
        private String apellido;

        @NotNull
        private LocalDate fechaNacimiento;

        @NotBlank
        @Pattern(regexp = "M|F|Otro")
        private String sexo;

        @Email
        @Size(max = 150)
        private String correo;

        @Size(max = 20)
        private String telefono;

        @Size(max = 500)
        private String fotoCarnetUrl;

        @Size(max = 50)
        private String numeroExpediente;

        private Integer usuarioId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepresentanteData {
        @NotBlank
        @Size(max = 20)
        private String cedula;

        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotBlank
        @Size(max = 100)
        private String apellido;

        @NotBlank
        @Pattern(regexp = "Padre|Madre|Tutor|Abuelo|Otro")
        private String parentesco;

        @NotBlank
        @Email
        @Size(max = 150)
        private String correo;

        @Size(max = 20)
        private String telefono;

        @Size(max = 20)
        private String telefonoSecundario;

        private String direccion;

        @Size(max = 100)
        private String ocupacion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreinscripcionResponseDTO {
        private Integer matriculaId;
        private String estadoMatricula;
        private LocalDate fechaSolicitud;
        private Integer estudianteId;
        private String estudianteNombreCompleto;
        private Integer representanteId;
        private String representanteNombreCompleto;
        private Integer seccionId;
        private Short anioEscolar;
        private Long cuposDisponibles;
    }
}