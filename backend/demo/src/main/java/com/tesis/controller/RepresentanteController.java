package com.tesis.controller;

import com.tesis.entity.Representante;
import com.tesis.dto.PaginacionDTO;
import com.tesis.service.RepresentanteService;
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
@RequestMapping("/api/representantes")
public class RepresentanteController {

    private final RepresentanteService representanteService;

    public RepresentanteController(RepresentanteService representanteService) {
        this.representanteService = representanteService;
    }

    @GetMapping
    public PaginacionDTO.Respuesta<Representante> listar(@ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(representanteService.listar(pageable));
    }

    @GetMapping("/{id}")
    public Representante obtener(@PathVariable Integer id) {
        return representanteService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Representante> crear(@Valid @RequestBody Representante representante) {
        return ResponseEntity.status(HttpStatus.CREATED).body(representanteService.crear(representante));
    }

    @PutMapping("/{id}")
    public Representante actualizar(@PathVariable Integer id, @Valid @RequestBody Representante representante) {
        return representanteService.actualizar(id, representante);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        representanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
