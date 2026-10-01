package com.tesis.controller;

import com.tesis.dto.UsuarioDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PaginacionDTO.Respuesta<UsuarioDTO.UsuarioResponseDTO> listar(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(usuarioService.listar(pageable));
    }

    @GetMapping("/{id}")
    public UsuarioDTO.UsuarioResponseDTO obtener(@PathVariable Integer id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO.UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioDTO.UsuarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public UsuarioDTO.UsuarioResponseDTO actualizar(@PathVariable Integer id,
                                                  @Valid @RequestBody UsuarioDTO.UsuarioRequestDTO request) {
        return usuarioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
