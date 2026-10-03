package com.tesis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class AsignacionTurnoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull
        private Integer empleadoId;

        @NotNull
        private Integer turnoId;

        @NotNull
        private LocalDate fechaDesde;

        private LocalDate fechaHasta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CambioRequest {
        @NotNull
        private Integer turnoId;

        @NotNull
        private LocalDate fechaDesde;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer id;
        private Integer empleadoId;
        private String empleadoNombre;
        private Integer turnoId;
        private String turnoNombre;
        private LocalDate fechaDesde;
        private LocalDate fechaHasta;
    }
}