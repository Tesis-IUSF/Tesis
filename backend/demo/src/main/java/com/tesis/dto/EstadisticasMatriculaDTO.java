package com.tesis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasMatriculaDTO {

    private long cuposDisponibles;
    private long cuposOcupados;
    private double porcentajeOcupacion;
    private Map<String, Long> estudiantesPorSexo;
    private List<EstadisticaSeccionDTO> estudiantesPorSeccion;
    private long nuevoIngreso;
    private long regularesPendientes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticaSeccionDTO {
        private String nivel;
        private String seccion;
        private Map<String, Long> estudiantesPorSexo;
        private long total;
    }
}