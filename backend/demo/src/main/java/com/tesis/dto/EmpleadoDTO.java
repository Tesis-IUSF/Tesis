package com.tesis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmpleadoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpleadoRequestDTO {
        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotBlank
        @Size(max = 100)
        private String apellido;

        @NotBlank
        @Size(max = 20)
        private String cedula;

        @Email
        @Size(max = 150)
        private String correo;

        @Size(max = 20)
        private String telefono;

        private LocalDate fechaNacimiento;

        @Pattern(regexp = "M|F|Otro")
        private String sexo;

        private Integer cargoId;
        private Integer departamentoId;
        private LocalDate fechaIngreso;
        private LocalDate fechaEgreso;
        private Integer usuarioId;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpleadoResponseDTO {
        private Integer id;
        private String nombre;
        private String apellido;
        private String cedula;
        private String correo;
        private String telefono;
        private LocalDate fechaNacimiento;
        private String sexo;
        private Integer cargoId;
        private String cargoNombre;
        private Integer departamentoId;
        private String departamentoNombre;
        private LocalDate fechaIngreso;
        private LocalDate fechaEgreso;
        private Integer usuarioId;
        private Boolean activo;
        private LocalDateTime creadoEn;
        private LocalDateTime actualizadoEn;
        private Integer turnoId;
        private String turnoNombre;
    }
}