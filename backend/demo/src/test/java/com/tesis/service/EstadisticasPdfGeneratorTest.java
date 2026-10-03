package com.tesis.service;

import com.tesis.dto.EstadisticasMatriculaDTO;
import com.tesis.dto.EstadisticasMatriculaDTO.EstadisticaSeccionDTO;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadisticasPdfGeneratorTest {

    @Test
    void generaDesglosePorNivelYSeccionConTotalesHembrasYVarones() throws Exception {
        EstadisticasPdfGenerator generator = new EstadisticasPdfGenerator();
        ReflectionTestUtils.setField(generator, "institucion", "Institución de prueba");
        Map<String, Long> porSexo = new LinkedHashMap<>();
        porSexo.put("F", 3L);
        porSexo.put("M", 2L);
        EstadisticasMatriculaDTO estadisticas = new EstadisticasMatriculaDTO(
                10,
                8,
                44.44,
                Map.of("F", 4L, "M", 4L),
                List.of(new EstadisticaSeccionDTO("Inicial", "Grupo I", porSexo, 5),
                        new EstadisticaSeccionDTO("Media General", "1er año A",
                                Map.of("F", 1L, "M", 2L), 3)),
                2,
                1);

        byte[] pdf = generator.generar(estadisticas);
        PdfReader reader = new PdfReader(pdf);
        String texto = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertTrue(texto.contains("INICIAL"));
        assertTrue(texto.contains("Grupo I"));
        assertTrue(texto.contains("MEDIA GENERAL"));
        assertTrue(texto.contains("1er año A"));
        assertTrue(texto.replaceAll("\\s+", " ")
                .contains("Total matrícula inscrita: F=4 M=4 Total=8"));
        assertTrue(!texto.contains("H="));
        assertTrue(!texto.contains("V="));
    }
}