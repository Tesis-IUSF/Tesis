package com.tesis.controller;

import com.tesis.dto.BlogDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/categorias")
    public PaginacionDTO.Respuesta<BlogDTO.CategoriaResponseDTO> listarCategoriasPublicas(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(blogService.listarCategoriasPublicas(paginacion.toPageable()));
    }

    @GetMapping("/publicaciones")
    public PaginacionDTO.Respuesta<BlogDTO.ComunicadoResponseDTO> listarPublicaciones(
            @RequestParam(required = false) Integer categoriaId,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(blogService.listarPublicaciones(categoriaId, paginacion.toPageable()));
    }

    @GetMapping("/publicaciones/{id}")
    public BlogDTO.ComunicadoResponseDTO obtenerPublicacion(@PathVariable Integer id) {
        return blogService.obtenerPublicacion(id);
    }

    @GetMapping("/admin/categorias")
    public PaginacionDTO.Respuesta<BlogDTO.CategoriaResponseDTO> listarCategoriasAdministrativas(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(blogService.listarCategorias(paginacion.toPageable()));
    }

    @PostMapping("/admin/categorias")
    public ResponseEntity<BlogDTO.CategoriaResponseDTO> crearCategoria(
            @Valid @RequestBody BlogDTO.CategoriaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogService.crearCategoria(request));
    }

    @PutMapping("/admin/categorias/{id}")
    public BlogDTO.CategoriaResponseDTO actualizarCategoria(@PathVariable Integer id,
                                                            @Valid @RequestBody BlogDTO.CategoriaRequestDTO request) {
        return blogService.actualizarCategoria(id, request);
    }

    @DeleteMapping("/admin/categorias/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        blogService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/comunicados")
    public PaginacionDTO.Respuesta<BlogDTO.ComunicadoResponseDTO> listarComunicados(
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Boolean publicado,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(blogService.listarComunicados(categoriaId, publicado, paginacion.toPageable()));
    }

    @GetMapping("/admin/comunicados/{id}")
    public BlogDTO.ComunicadoResponseDTO obtenerComunicado(@PathVariable Integer id) {
        return blogService.obtenerComunicado(id);
    }

    @PostMapping("/admin/comunicados")
    public ResponseEntity<BlogDTO.ComunicadoResponseDTO> crearComunicado(
            @Valid @RequestBody BlogDTO.ComunicadoRequestDTO request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blogService.crearComunicado(request, principal.getName()));
    }

    @PutMapping("/admin/comunicados/{id}")
    public BlogDTO.ComunicadoResponseDTO actualizarComunicado(@PathVariable Integer id,
                                                              @Valid @RequestBody BlogDTO.ComunicadoRequestDTO request) {
        return blogService.actualizarComunicado(id, request);
    }

    @DeleteMapping("/admin/comunicados/{id}")
    public ResponseEntity<Void> eliminarComunicado(@PathVariable Integer id) {
        blogService.eliminarComunicado(id);
        return ResponseEntity.noContent().build();
    }

    private <T> PaginacionDTO.Respuesta<T> paginar(Page<T> pagina) {
        return PaginacionDTO.Respuesta.desde(pagina);
    }
}