package com.tesis.controller;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AsistenciaHoyDTO;
import com.tesis.dto.AsistenciaDTO.AusenciaDTO;
import com.tesis.dto.AsistenciaDTO.EscaneoQrRequestDTO;
import com.tesis.service.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/qr")
    @ResponseStatus(HttpStatus.CREATED)
    public AsistenciaResponseDTO registrarEscaneo(@Valid @RequestBody EscaneoQrRequestDTO request) {
        return asistenciaService.registrarEscaneo(request.getQrToken());
    }

    @GetMapping("/hoy")
    public List<AsistenciaHoyDTO> listarHoy() {
        return asistenciaService.listarHoy();
    }

    @GetMapping("/historico")
    public List<AsistenciaResponseDTO> buscarHistorico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer empleadoId,
            @RequestParam(required = false) Integer departamentoId,
            @RequestParam(required = false) Integer turnoId,
            @RequestParam(required = false) String estado) {
        return asistenciaService.buscarHistorico(desde, hasta, empleadoId,
                departamentoId, turnoId, estado);
    }

    @GetMapping("/ausencias")
    public List<AusenciaDTO> listarAusencias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer empleadoId,
            @RequestParam(required = false) Integer departamentoId,
            @RequestParam(required = false) Integer turnoId) {
        return asistenciaService.listarAusencias(desde, hasta, empleadoId, departamentoId, turnoId);
    }

    @GetMapping("/empleado/{empleadoId}")
    public List<AsistenciaResponseDTO> listarPorEmpleado(
            @PathVariable Integer empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return asistenciaService.listarPorEmpleado(empleadoId, desde, hasta);
    }
}