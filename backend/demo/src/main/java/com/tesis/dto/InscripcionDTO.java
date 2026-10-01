package com.tesis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InscripcionDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistUpdateRequestDTO {
        @NotNull
        private Boolean cumplido;

        private String notas;

        @Size(max = 500)
        private String archivoUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormalizarInscripcionRequestDTO {
        @Size(max = 50)
        private String numeroConstancia;

        @Size(max = 500)
        private String constanciaUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItemResponseDTO {
        private Integer checklistId;
        private Integer requisitoId;
        private String nombreRequisito;
        private String descripcionRequisito;
        private Boolean obligatorio;
        private Boolean cumplido;
        private LocalDateTime fechaVerificacion;
        private Integer verificadoPorId;
        private String notas;
        private String archivoUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InscripcionResponseDTO {
        private Integer matriculaId;
        private String estadoMatricula;
        private LocalDate fechaSolicitud;
        private LocalDate fechaFormalizacion;
        private String numeroConstancia;
        private String constanciaUrl;
        private Integer estudianteId;
        private String estudianteNombreCompleto;
        private Integer seccionId;
        private Short anioEscolar;
        private long requisitosObligatorios;
        private long requisitosObligatoriosCumplidos;
        private List<ChecklistItemResponseDTO> checklist;
    }
}