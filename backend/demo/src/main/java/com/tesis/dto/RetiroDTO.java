package com.tesis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RetiroDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetiroRequestDTO {
        @NotBlank
        private String motivo;

        @NotNull
        private LocalDate fechaRetiro;

        private Integer representanteId;

        @Size(max = 100)
        private String solicitanteNombre;

        @Size(max = 100)
        private String solicitanteApellido;

        @Size(max = 20)
        private String solicitanteCedula;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetiroResponseDTO {
        private Integer retiroId;
        private Integer matriculaId;
        private Integer estudianteId;
        private String estudianteNombreCompleto;
        private Short anioEscolar;
        private String estadoMatricula;
        private LocalDate fechaRetiro;
        private String motivo;
        private Integer representanteId;
        private String solicitanteNombre;
        private String solicitanteApellido;
        private String solicitanteCedula;
        private Integer procesadoPorId;
        private LocalDateTime creadoEn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistorialRetirosResponseDTO {
        private Integer matriculaId;
        private List<RetiroResponseDTO> retiros;
    }
}