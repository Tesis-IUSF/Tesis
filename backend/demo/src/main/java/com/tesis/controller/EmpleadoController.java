package com.tesis.controller;

import com.tesis.dto.EmpleadoDTO.EmpleadoRequestDTO;
import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.service.EmpleadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public PaginacionDTO.Respuesta<EmpleadoResponseDTO> listar(@ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(empleadoService.listar(pageable));
    }

    @GetMapping("/{id}")
    public EmpleadoResponseDTO obtener(@PathVariable Integer id) {
        return empleadoService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponseDTO> crear(@Valid @RequestBody EmpleadoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crear(request));
    }

    @PutMapping("/{id}")
    public EmpleadoResponseDTO actualizar(@PathVariable Integer id,
                                          @Valid @RequestBody EmpleadoRequestDTO request) {
        return empleadoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        empleadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}