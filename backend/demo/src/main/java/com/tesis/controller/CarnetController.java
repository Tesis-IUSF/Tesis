package com.tesis.controller;

import com.tesis.service.CarnetService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empleados")
public class CarnetController {

    private final CarnetService carnetService;

    public CarnetController(CarnetService carnetService) {
        this.carnetService = carnetService;
    }

    @PostMapping(value = "/{id}/carnet", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarCarnet(@PathVariable Integer id) {
        byte[] pdf = carnetService.generarCarnet(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("carnet-empleado-" + id + ".pdf").build().toString())
                .body(pdf);
    }
}