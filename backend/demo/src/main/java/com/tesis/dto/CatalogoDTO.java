package com.tesis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

public class CatalogoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartamentoRequestDTO {
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartamentoResponseDTO {
        private Integer id;
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CargoRequestDTO {
        private String nombreCargo;
        private Integer departamentoId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CargoResponseDTO {
        private Integer id;
        private String nombreCargo;
        private Integer departamentoId;
        private String nombreDepartamento;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TurnoRequestDTO {
        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotNull
        private LocalTime horaEntrada;

        @NotNull
        private LocalTime horaSalida;

        @Min(0)
        @Max(180)
        private Short toleranciaMin;

        @Min(0)
        @Max(180)
        private Short minutosSalidaAnticipadaPermitidos;
        private Boolean requiereJustificacionTardanza;
        private Boolean lunes;
        private Boolean martes;
        private Boolean miercoles;
        private Boolean jueves;
        private Boolean viernes;
        private Boolean sabado;
        private Boolean domingo;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TurnoResponseDTO {
        private Integer id;
        private String nombre;
        private LocalTime horaEntrada;
        private LocalTime horaSalida;
        private Short toleranciaMin;
        private Short minutosSalidaAnticipadaPermitidos;
        private Boolean requiereJustificacionTardanza;
        private Boolean lunes;
        private Boolean martes;
        private Boolean miercoles;
        private Boolean jueves;
        private Boolean viernes;
        private Boolean sabado;
        private Boolean domingo;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NivelEducativoRequestDTO {
        private String nombre;
        private String descripcion;
        private Short duracionAnios;
        private Short ordinal;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NivelEducativoResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Short duracionAnios;
        private Short ordinal;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GradoRequestDTO {
        @NotNull
        private Integer nivelEducativoId;
        @NotNull
        @Min(1)
        @Max(12)
        private Short numeroGrado;
        @NotNull
        @Min(1)
        @Max(20)
        private Short edadMinima;
        @NotNull
        @Min(1)
        @Max(20)
        private Short edadMaxima;
        private String nombreEspecial;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GradoResponseDTO {
        private Integer id;
        private Integer nivelEducativoId;
        private String nivelEducativoNombre;
        private Short numeroGrado;
        private Short edadMinima;
        private Short edadMaxima;
        private String nombreEspecial;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeccionRequestDTO {
        @NotNull
        private Integer gradoId;
        @NotBlank
        @Pattern(regexp = "[A-Z]")
        @Size(max = 1)
        private String letraSeccion;
        @NotNull
        @Min(1)
        @Max(60)
        private Short capacidadMaxima;
        private Integer docentePrincipalId;
        private Integer turnoId;
        @NotNull
        @Min(1)
        private Short anioEscolar;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeccionResponseDTO {
        private Integer id;
        private Integer gradoId;
        private String gradoNombre;
        private String letraSeccion;
        private Short capacidadMaxima;
        private Integer docentePrincipalId;
        private String docentePrincipalNombre;
        private Integer turnoId;
        private Short anioEscolar;
        private Boolean activo;
        private Long matriculados;
        private Long cuposDisponibles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocenteOptionDTO {
        private Integer id;
        private String nombre;
        private String apellido;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequisitoMatriculaRequestDTO {
        private String nombre;
        private String descripcion;
        private Boolean obligatorio;
        private String aplicaTipoIngreso;
        private Integer nivelEducativoId;
        private String documentoTemplateUrl;
        private Boolean activo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequisitoMatriculaResponseDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private Boolean obligatorio;
        private String aplicaTipoIngreso;
        private Integer nivelEducativoId;
        private String nivelEducativoNombre;
        private String documentoTemplateUrl;
        private Boolean activo;
    }
}
