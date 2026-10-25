package com.tesis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmpleadoDTO {

    public interface CrearEmpleado extends Default {
    }

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

        @Size(max = 500)
        private String fotoUrl;

        @NotNull(message = "El cargo es obligatorio")
        private Integer cargoId;
        private Integer departamentoId;
        @NotNull(message = "La fecha de ingreso es obligatoria")
        private LocalDate fechaIngreso;
        private LocalDate fechaEgreso;
        private Integer usuarioId;
        private Boolean activo;

        @NotNull(message = "El turno es obligatorio al crear un empleado", groups = CrearEmpleado.class)
        private Integer turnoId;
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
        private String fotoUrl;
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