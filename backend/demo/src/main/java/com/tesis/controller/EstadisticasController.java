package com.tesis.controller;

import com.tesis.dto.EstadisticasMatriculaDTO;
import com.tesis.service.EstadisticasMatriculaService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/director/estadisticas")
public class EstadisticasController {

    private final EstadisticasMatriculaService estadisticasMatriculaService;

    public EstadisticasController(EstadisticasMatriculaService estadisticasMatriculaService) {
        this.estadisticasMatriculaService = estadisticasMatriculaService;
    }

    @GetMapping
    public EstadisticasMatriculaDTO obtenerEstadisticasGenerales() {
        return estadisticasMatriculaService.obtenerEstadisticasGenerales();
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarEstadisticasGeneralesPDF() {
        byte[] pdf = estadisticasMatriculaService.exportarEstadisticasGeneralesPDF();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("estadisticas-matricula.pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/seccion/{id}")
    public EstadisticasMatriculaDTO obtenerEstadisticasSeccion(@PathVariable("id") Integer id) {
        return estadisticasMatriculaService.obtenerEstadisticasSeccion(id);
    }

    @GetMapping("/nivel/{nivel}")
    public EstadisticasMatriculaDTO obtenerEstadisticasNivel(@PathVariable("nivel") String nivel) {
        return estadisticasMatriculaService.obtenerEstadisticasNivel(nivel);
    }
}