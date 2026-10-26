package com.tesis.controller;

import com.tesis.dto.InitialSetupDTO.InitialAdminRequestDTO;
import com.tesis.dto.InitialSetupDTO.InitialAdminResponseDTO;
import com.tesis.dto.InitialSetupDTO.StatusResponseDTO;
import com.tesis.service.InitialSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
public class InitialSetupController {

    private final InitialSetupService initialSetupService;

    public InitialSetupController(InitialSetupService initialSetupService) {
        this.initialSetupService = initialSetupService;
    }

    @GetMapping("/status")
    public StatusResponseDTO obtenerEstado() {
        return initialSetupService.obtenerEstado();
    }

    @PostMapping("/initial-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public InitialAdminResponseDTO crearAdministradorInicial(
            @Valid @RequestBody InitialAdminRequestDTO request,
            @RequestHeader(value = "X-Initial-Admin-Token", required = false) String token) {
        return initialSetupService.crearAdministradorInicial(request, token);
    }
}
