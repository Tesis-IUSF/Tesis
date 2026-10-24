package com.tesis.dto;

import com.tesis.entity.TipoPeriodoMatricula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public final class GestionCalendarioDTO {

    private GestionCalendarioDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnioEscolarRequestDTO {
        @NotNull
        @Min(1900)
        @Max(2200)
        private Short anio;

        @NotBlank
        @Size(max = 20)
        private String nombre;

        @NotNull
        private LocalDate fechaInicio;

        @NotNull
        private LocalDate fechaFin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnioEscolarResponseDTO {
        private Short anio;
        private String nombre;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private boolean activo;
        private Short sugeridoDesdeAnio;
        private List<PeriodoResponseDTO> periodos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodoRequestDTO {
        @NotNull
        private TipoPeriodoMatricula tipo;

        @NotNull
        private LocalDate fechaInicio;

        @NotNull
        private LocalDate fechaFin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodoResponseDTO {
        private Integer id;
        private Short anioEscolar;
        private TipoPeriodoMatricula tipo;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private boolean activo;
    }
}
