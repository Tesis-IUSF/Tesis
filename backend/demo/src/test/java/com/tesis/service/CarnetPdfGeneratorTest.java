package com.tesis.service;

import com.tesis.entity.Empleado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.openpdf.text.pdf.PdfReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarnetPdfGeneratorTest {

    private static final Pattern IMAGE_OBJECT = Pattern.compile("/Subtype\\s*/Image");

    @TempDir
    Path uploadsDirectory;

    @Test
    void incluyeLaFotoDelEmpleadoEnElCarnet() throws Exception {
        FotoService fotoService = new FotoService(uploadsDirectory.toString());
        byte[] imagen = crearImagenPng();
        String fotoUrl = fotoService.guardar(new MockMultipartFile(
                "archivo", "foto.png", "image/png", imagen)).url();
        CarnetPdfGenerator generator = new CarnetPdfGenerator(fotoService, "Escuela");

        byte[] pdf = generator.generar(empleado(fotoUrl), "qr-token");

        assertEquals(2, contarImagenes(pdf));
    }

    @Test
    void muestraUnMarcadorConInicialesCuandoElEmpleadoNoTieneFoto() throws Exception {
        CarnetPdfGenerator generator = new CarnetPdfGenerator(
                new FotoService(uploadsDirectory.toString()), "Escuela");

        byte[] pdf = generator.generar(empleado(null), "qr-token");

        PdfReader reader = new PdfReader(pdf);
        assertTrue(new String(reader.getPageContent(1), java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains("SIN FOTO"));
        reader.close();
        assertEquals(1, contarImagenes(pdf));
    }

    private Empleado empleado(String fotoUrl) {
        Empleado empleado = new Empleado();
        empleado.setId(1);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setFotoUrl(fotoUrl);
        return empleado;
    }

    private byte[] crearImagenPng() throws Exception {
        BufferedImage imagen = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", output);
        return output.toByteArray();
    }

    private int contarImagenes(byte[] pdf) {
        Matcher matcher = IMAGE_OBJECT.matcher(
                new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1));
        int cantidad = 0;
        while (matcher.find()) {
            cantidad++;
        }
        return cantidad;
    }
}
