package com.tesis.controller;

import com.tesis.dto.FotoResponseDTO;
import com.tesis.service.FotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    private final FotoService fotoService;

    public FotoController(FotoService fotoService) {
        this.fotoService = fotoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoResponseDTO subir(@RequestPart("archivo") MultipartFile archivo) {
        return fotoService.guardar(archivo);
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<byte[]> obtener(@PathVariable String nombre) {
        FotoService.Imagen imagen = fotoService.almacenada(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagen.contenido());
    }
}
