package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AusenciaDTO;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenciaPdfGeneratorTest {

    @Test
    void generaReporteHorizontalConResumenDetalleYAusencias() throws Exception {
        AsistenciaPdfGenerator generator = new AsistenciaPdfGenerator();
        ReflectionTestUtils.setField(generator, "institucion", "Institución de prueba");
        LocalDate fecha = LocalDate.of(2026, 10, 26);
        AsistenciaResponseDTO registro = new AsistenciaResponseDTO(
                1, 10, "Ana Pérez", "V-12345678", fecha, 3, "Matutino",
                LocalTime.of(8, 15), LocalTime.of(16, 0), "tardanza",
                (short) 15, (short) 30, null, null);
        AusenciaDTO ausencia = new AusenciaDTO(
                11, "Luis", "Gómez", "V-87654321", fecha.minusDays(1),
                3, "Matutino", "ausente");

        byte[] pdf = generator.generar(fecha, fecha, List.of(registro), List.of(ausencia), true);
        PdfReader reader = new PdfReader(pdf);
        StringBuilder contenido = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            assertTrue(reader.getPageSizeWithRotation(page).getWidth()
                    > reader.getPageSizeWithRotation(page).getHeight());
            contenido.append(new PdfTextExtractor(reader).getTextFromPage(page)).append(' ');
        }
        reader.close();

        String texto = contenido.toString().replaceAll("\\s+", " ");
        assertTrue(texto.contains("REPORTE HISTÓRICO DE ASISTENCIA"));
        assertTrue(texto.contains("Período: 26/10/2026 al 26/10/2026"));
        assertTrue(texto.contains("Asistencias"));
        assertTrue(texto.contains("Inasistencias"));
        assertTrue(texto.contains("Tardanzas"));
        assertTrue(texto.contains("Horas trabajadas"));
        assertTrue(texto.contains("Empleado"));
        assertTrue(texto.contains("Cédula"));
        assertTrue(texto.contains("Min. tarde"));
        assertTrue(texto.contains("Min. extra"));
        assertTrue(texto.contains("Tipo de registro"));
        assertTrue(texto.contains("Ana Pérez"));
        assertTrue(texto.contains("V-12345678"));
        assertTrue(texto.contains("Entrada y salida"));
        assertTrue(texto.contains("Luis Gómez"));
        assertTrue(texto.contains("Sin marcaje"));
    }
}
