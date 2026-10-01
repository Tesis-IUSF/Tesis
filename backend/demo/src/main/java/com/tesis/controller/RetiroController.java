package com.tesis.controller;

import com.tesis.dto.RetiroDTO.HistorialRetirosResponseDTO;
import com.tesis.dto.RetiroDTO.RetiroRequestDTO;
import com.tesis.dto.RetiroDTO.RetiroResponseDTO;
import com.tesis.service.RetiroService;
import com.tesis.service.ConstanciaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/matriculas/{matriculaId}/retiros")
public class RetiroController {

    private final RetiroService retiroService;
    private final ConstanciaService constanciaService;

    public RetiroController(RetiroService retiroService, ConstanciaService constanciaService) {
        this.retiroService = retiroService;
        this.constanciaService = constanciaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RetiroResponseDTO retirar(@PathVariable Integer matriculaId,
                                     @Valid @RequestBody RetiroRequestDTO request,
                                     Principal principal) {
        return retiroService.retirar(matriculaId, request, principal.getName());
    }

    @GetMapping
    public HistorialRetirosResponseDTO obtenerHistorial(@PathVariable Integer matriculaId) {
        return retiroService.obtenerHistorial(matriculaId);
    }

    @GetMapping(value = "/{retiroId}/constancia-retiro", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarConstanciaRetiro(@PathVariable Integer matriculaId,
                                                           @PathVariable Integer retiroId) {
        byte[] pdf = constanciaService.generarConstanciaRetiro(matriculaId, retiroId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("constancia-retiro-" + retiroId + ".pdf").build().toString())
                .body(pdf);
    }
}