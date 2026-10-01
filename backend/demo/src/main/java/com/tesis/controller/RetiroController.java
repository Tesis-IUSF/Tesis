package com.tesis.controller;

import com.tesis.dto.RetiroDTO.HistorialRetirosResponseDTO;
import com.tesis.dto.RetiroDTO.RetiroRequestDTO;
import com.tesis.dto.RetiroDTO.RetiroResponseDTO;
import com.tesis.service.RetiroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public RetiroController(RetiroService retiroService) {
        this.retiroService = retiroService;
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
}