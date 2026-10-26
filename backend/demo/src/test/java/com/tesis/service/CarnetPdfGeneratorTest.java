package com.tesis.service;

import com.tesis.entity.Empleado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.openpdf.text.pdf.PdfReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        byte[] pdf = generator.generar(empleado(fotoUrl, "M"), "qr-token");

        // foto del empleado + QR
        assertEquals(2, contarImagenes(pdf));
    }

    @Test
    void usaLaSiluetaCuandoElEmpleadoNoTieneFotoYSuSexoEsMasculinoOFemenino() throws Exception {
        CarnetPdfGenerator generator = new CarnetPdfGenerator(
                new FotoService(uploadsDirectory.toString()), "Escuela");

        byte[] pdfHombre = generator.generar(empleado(null, "M"), "qr-token");
        byte[] pdfMujer = generator.generar(empleado(null, "F"), "qr-token");

        // silueta + QR; ya no se muestra el marcador "SIN FOTO"
        assertEquals(2, contarImagenes(pdfHombre));
        assertEquals(2, contarImagenes(pdfMujer));
        assertFalse(contenido(pdfHombre).contains("SIN FOTO"));
        assertFalse(contenido(pdfMujer).contains("SIN FOTO"));
    }

    @Test
    void usaLaSiluetaNeutralCuandoNoHayFotoNiSexoDefinido() throws Exception {
        CarnetPdfGenerator generator = new CarnetPdfGenerator(
                new FotoService(uploadsDirectory.toString()), "Escuela");

        byte[] sinSexo = generator.generar(empleado(null, null), "qr-token");
        byte[] otro = generator.generar(empleado(null, "Otro"), "qr-token");

        assertEquals(2, contarImagenes(sinSexo));
        assertEquals(2, contarImagenes(otro));
        assertFalse(contenido(sinSexo).contains("SIN FOTO"));
    }

    @Test
    void muestraLaCedulaDeIdentidadDelEmpleado() throws Exception {
        CarnetPdfGenerator generator = new CarnetPdfGenerator(
                new FotoService(uploadsDirectory.toString()), "Escuela");

        byte[] pdf = generator.generar(empleado(null, "F"), "qr-token");

        assertTrue(contenido(pdf).contains("C.I.: V-12345678"));
    }

    private Empleado empleado(String fotoUrl, String sexo) {
        Empleado empleado = new Empleado();
        empleado.setId(1);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setCedula("V-12345678");
        empleado.setSexo(sexo);
        empleado.setFotoUrl(fotoUrl);
        return empleado;
    }

    private String contenido(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        } finally {
            reader.close();
        }
    }

    private byte[] crearImagenPng() throws Exception {
        BufferedImage imagen = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", output);
        return output.toByteArray();
    }

    private int contarImagenes(byte[] pdf) {
        Matcher matcher = IMAGE_OBJECT.matcher(
                new String(pdf, StandardCharsets.ISO_8859_1));
        int cantidad = 0;
        while (matcher.find()) {
            cantidad++;
        }
        return cantidad;
    }
}