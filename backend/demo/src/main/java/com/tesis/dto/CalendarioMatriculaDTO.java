package com.tesis.dto;

import com.tesis.entity.TipoPeriodoMatricula;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioMatriculaDTO {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private AnioEscolarDTO anioActivo;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private AnioEscolarDTO anioConsultado;
    private List<PeriodoDTO> periodos;
    private String mensaje;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnioEscolarDTO {
        private Short anio;
        private String nombre;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodoDTO {
        private Integer id;
        private Short anioEscolar;
        private TipoPeriodoMatricula tipo;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private boolean vigente;
    }
}