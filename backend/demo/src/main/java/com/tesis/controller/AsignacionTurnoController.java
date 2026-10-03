package com.tesis.controller;

import com.tesis.dto.AsignacionTurnoDTO;
import com.tesis.service.AsignacionTurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-turnos")
public class AsignacionTurnoController {

    private final AsignacionTurnoService asignacionTurnoService;

    public AsignacionTurnoController(AsignacionTurnoService asignacionTurnoService) {
        this.asignacionTurnoService = asignacionTurnoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionTurnoDTO.Response asignar(@Valid @RequestBody AsignacionTurnoDTO.Request request) {
        return asignacionTurnoService.asignar(request);
    }

    @PutMapping("/empleado/{empleadoId}/vigente")
    public AsignacionTurnoDTO.Response reemplazarVigente(@PathVariable Integer empleadoId,
                                                         @Valid @RequestBody AsignacionTurnoDTO.CambioRequest request) {
        return asignacionTurnoService.reemplazarVigente(empleadoId, request);
    }

    @GetMapping("/empleado/{empleadoId}")
    public List<AsignacionTurnoDTO.Response> listarPorEmpleado(@PathVariable Integer empleadoId) {
        return asignacionTurnoService.listarPorEmpleado(empleadoId);
    }
}