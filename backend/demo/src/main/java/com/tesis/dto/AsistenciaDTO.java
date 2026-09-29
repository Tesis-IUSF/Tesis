package com.tesis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AsistenciaDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscaneoQrRequestDTO {
        @NotNull
        @Positive
        private Integer empleadoId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsistenciaResponseDTO {
        private Integer id;
        private Integer empleadoId;
        private String empleadoNombre;
        private LocalDate fecha;
        private Integer turnoId;
        private String turnoNombre;
        private LocalTime horaEntrada;
        private LocalTime horaSalida;
        private String estado;
        private Short minutosTardanza;
        private Short minutosExtra;
        private String tipoRegistro;
        private LocalDateTime creadoEn;
    }
}