package com.tesis.controller;

import com.tesis.dto.InscripcionDTO.ChecklistUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.FormalizarInscripcionRequestDTO;
import com.tesis.dto.InscripcionDTO.InscripcionResponseDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionRequestDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionResponseDTO;
import com.tesis.service.InscripcionService;
import com.tesis.service.PreinscripcionService;
import com.tesis.service.ConstanciaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final PreinscripcionService preinscripcionService;
    private final InscripcionService inscripcionService;
    private final ConstanciaService constanciaService;

    public MatriculaController(PreinscripcionService preinscripcionService,
                               InscripcionService inscripcionService,
                               ConstanciaService constanciaService) {
        this.preinscripcionService = preinscripcionService;
        this.inscripcionService = inscripcionService;
        this.constanciaService = constanciaService;
    }

    @PostMapping("/preinscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PreinscripcionResponseDTO preinscribir(
            @Valid @RequestBody PreinscripcionRequestDTO request) {
        return preinscripcionService.preinscribir(request);
    }

    @GetMapping("/preinscripciones")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarPreinscritas(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(inscripcionService.listarPreinscritas(pageable));
    }

    @GetMapping("/en-proceso")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarEnProceso(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(inscripcionService.listarEnProceso(pageable));
    }

    @GetMapping("/{matriculaId}/checklist")
    public InscripcionResponseDTO obtenerSeguimiento(@PathVariable Integer matriculaId) {
        return inscripcionService.obtenerSeguimiento(matriculaId);
    }

    @PostMapping("/{matriculaId}/checklist")
    public InscripcionResponseDTO prepararChecklist(@PathVariable Integer matriculaId) {
        return inscripcionService.prepararChecklist(matriculaId);
    }

    @PatchMapping("/{matriculaId}/checklist/{requisitoId}")
    public InscripcionResponseDTO actualizarRequisito(
            @PathVariable Integer matriculaId,
            @PathVariable Integer requisitoId,
            @Valid @RequestBody ChecklistUpdateRequestDTO request,
            Principal principal) {
        return inscripcionService.actualizarRequisito(matriculaId, requisitoId, request, principal.getName());
    }

    @PostMapping("/{matriculaId}/formalizar")
    public InscripcionResponseDTO formalizar(
            @PathVariable Integer matriculaId,
            @Valid @RequestBody(required = false) FormalizarInscripcionRequestDTO request) {
        return inscripcionService.formalizar(matriculaId, request);
    }

    @GetMapping(value = "/{matriculaId}/constancia-asignacion-cupo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarConstanciaAsignacionCupo(@PathVariable Integer matriculaId) {
        byte[] pdf = constanciaService.generarConstanciaAsignacionCupo(matriculaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename("constancia-aceptacion-cupo-" + matriculaId + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping(value = "/{matriculaId}/constancia-inscripcion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarConstanciaInscripcion(@PathVariable Integer matriculaId) {
        byte[] pdf = constanciaService.generarConstanciaInscripcion(matriculaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("constancia-inscripcion-" + matriculaId + ".pdf").build().toString())
                .body(pdf);
    }
}