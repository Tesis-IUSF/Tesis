package com.tesis.service;

import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NominaPersonalPdfGeneratorTest {

    @Test
    void generaNominaHorizontalConColumnasYFechaDeEgresoCondicional() throws Exception {
        NominaPersonalPdfGenerator generator = new NominaPersonalPdfGenerator();
        ReflectionTestUtils.setField(generator, "institucion", "Institución de prueba");
        ReflectionTestUtils.setField(generator, "municipio", "San Francisco");
        ReflectionTestUtils.setField(generator, "estado", "Zulia");
        ReflectionTestUtils.setField(generator, "codigoPlantel", "OD34942317");
        EmpleadoResponseDTO empleado = new EmpleadoResponseDTO(
                1, "Ana", "Pérez", "V-12345678", "ana@example.com", "04121234567",
                LocalDate.of(1990, 3, 5), "F", null, 2, "Docente", 3, "Académico",
                LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 30), null, false,
                null, null, 4, "Matutino");

        byte[] pdfConEgreso = generator.generar(List.of(empleado), true);
        PdfReader readerConEgreso = new PdfReader(pdfConEgreso);
        String textoConEgreso = texto(readerConEgreso).replaceAll("\\s+", " ");
        assertTrue(readerConEgreso.getPageSizeWithRotation(1).getWidth()
                > readerConEgreso.getPageSizeWithRotation(1).getHeight());
        assertTrue(textoConEgreso.contains("NÓMINA DE PERSONAL"));
        assertTrue(textoConEgreso.contains("República Bolivariana de Venezuela"), textoConEgreso);
        assertTrue(textoConEgreso.contains("Ministerio del Poder Popular para la Educación"), textoConEgreso);
        assertTrue(textoConEgreso.contains("Municipio San Francisco"));
        assertTrue(textoConEgreso.contains("Cod. Plantel: OD34942317"));
        assertTrue(textoConEgreso.contains("Cédula"));
        assertTrue(textoConEgreso.contains("Fecha de nacimiento"));
        assertTrue(textoConEgreso.contains("Correo"));
        assertTrue(textoConEgreso.contains("Departamento"));
        assertTrue(textoConEgreso.contains("Fecha de ingreso"));
        assertTrue(textoConEgreso.contains("Fecha de egreso"));
        assertTrue(textoConEgreso.contains("30/06/2024"));
        assertTrue(textoConEgreso.contains("ana@example.com"));
        readerConEgreso.close();

        byte[] pdfSinEgreso = generator.generar(List.of(empleado), false);
        PdfReader readerSinEgreso = new PdfReader(pdfSinEgreso);
        String textoSinEgreso = texto(readerSinEgreso).replaceAll("\\s+", " ");
        assertFalse(textoSinEgreso.contains("Fecha de egreso"));
        assertTrue(textoSinEgreso.contains("Matutino"));
        readerSinEgreso.close();
    }

    private String texto(PdfReader reader) throws Exception {
        StringBuilder contenido = new StringBuilder();
        for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
            contenido.append(new PdfTextExtractor(reader).getTextFromPage(pagina)).append(' ');
        }
        return contenido.toString();
    }
}
