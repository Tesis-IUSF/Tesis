package com.tesis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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
        @Pattern(regexp = "^(?:(?i:https?)://\\S+)?$")
        private String archivoUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistBatchUpdateRequestDTO {
        @NotEmpty
        @Valid
        private List<ChecklistBatchItemRequestDTO> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistBatchItemRequestDTO {
        @NotNull
        private Integer requisitoId;

        @NotNull
        private Boolean cumplido;

        private String notas;

        @Size(max = 500)
        @Pattern(regexp = "^(?:(?i:https?)://\\S+)?$")
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
        private String estudianteCedula;
        private Integer seccionId;
        private Short anioEscolar;
        private long requisitosObligatorios;
        private long requisitosObligatoriosCumplidos;
        private List<ChecklistItemResponseDTO> checklist;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstudianteExpedienteDTO {
        private Integer id;
        private String cedula;
        private String nombre;
        private String apellido;
        private LocalDate fechaNacimiento;
        private String sexo;
        private String correo;
        private String telefono;
        private String numeroExpediente;
        private String observaciones;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepresentanteExpedienteDTO {
        private Integer id;
        private String cedula;
        private String nombre;
        private String apellido;
        private String parentesco;
        private String correo;
        private String telefono;
        private String telefonoSecundario;
        private String direccion;
        private String ocupacion;
        private Boolean relacionPrincipal;
        private Boolean autorizadoRetirar;
        private Boolean recibeComunicados;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpedienteResponseDTO {
        private Integer matriculaId;
        private String estadoMatricula;
        private Short anioEscolar;
        private LocalDate fechaSolicitud;
        private LocalDate fechaFormalizacion;
        private String numeroConstancia;
        private String tipoIngreso;
        private String institucionProcedencia;
        private Integer seccionId;
        private String nivelEducativoNombre;
        private String gradoNombre;
        private String letraSeccion;
        private EstudianteExpedienteDTO estudiante;
        private List<RepresentanteExpedienteDTO> representantes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatriculaAcademicaDTO {
        private Integer matriculaId;
        private Short anioEscolar;
        private String estadoMatricula;
        private LocalDate fechaSolicitud;
        private LocalDate fechaFormalizacion;
        private String nivelEducativoNombre;
        private String gradoNombre;
        private String letraSeccion;
    }
}