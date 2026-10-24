package com.tesis.service;

import com.tesis.dto.FotoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FotoServiceTest {

    @TempDir
    Path directorioTemporal;

    private FotoService fotoService;

    @BeforeEach
    void setUp() {
        fotoService = new FotoService(directorioTemporal.toString());
    }

    @Test
    void guardaUnaImagenPngConNombreAleatorioYLaSirve() throws IOException {
        byte[] contenido = imagenPng();
        FotoResponseDTO respuesta = fotoService.guardar(
                new MockMultipartFile("archivo", "foto.png", "image/png", contenido));

        String nombre = respuesta.url().substring("/api/fotos/".length());
        UUID.fromString(nombre.substring(0, nombre.length() - ".png".length()));
        FotoService.Imagen imagen = fotoService.almacenada(nombre);

        assertEquals("image/png", imagen.contentType());
        assertArrayEquals(contenido, imagen.contenido());
    }

    @Test
    void rechazaContenidoQueNoEsImagenAunqueDeclareMimeDeImagen() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fotoService.guardar(new MockMultipartFile(
                        "archivo", "foto.png", "image/png", "no es una imagen".getBytes())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rechazaImagenMayorDeDosMegabytes() {
        byte[] contenido = new byte[2 * 1024 * 1024 + 1];
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fotoService.guardar(new MockMultipartFile(
                        "archivo", "foto.png", "image/png", contenido)));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, exception.getStatusCode());
    }

    private byte[] imagenPng() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
