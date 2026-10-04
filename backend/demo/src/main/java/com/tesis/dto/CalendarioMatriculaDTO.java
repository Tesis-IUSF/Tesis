package com.tesis.dto;

import com.tesis.entity.TipoPeriodoMatricula;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioMatriculaDTO {

    private AnioEscolarDTO anioActivo;
    private AnioEscolarDTO anioConsultado;
    private List<PeriodoDTO> periodos;

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