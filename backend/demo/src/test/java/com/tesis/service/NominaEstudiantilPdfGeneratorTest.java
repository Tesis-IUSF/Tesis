package com.tesis.service;

import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.Representante;
import com.tesis.entity.Seccion;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NominaEstudiantilPdfGeneratorTest {

    @Test
    void generaNominaHorizontalConMembreteDatosYColumnasSeleccionadas() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
        NominaEstudiantilPdfGenerator generator = new NominaEstudiantilPdfGenerator(clock);
        ReflectionTestUtils.setField(generator, "institucion", "Institución de prueba");
        ReflectionTestUtils.setField(generator, "municipio", "San Francisco");
        ReflectionTestUtils.setField(generator, "estado", "Zulia");
        ReflectionTestUtils.setField(generator, "codigoPlantel", "OD34942317");

        Estudiante estudiante = new Estudiante();
        estudiante.setId(7);
        estudiante.setCedula("V-12345678");
        estudiante.setNombre("Ana");
        estudiante.setApellido("Pérez");
        estudiante.setFechaNacimiento(LocalDate.of(2010, 2, 28));
        estudiante.setSexo("F");

        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Media General");
        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 3);
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion("A");

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setTipoIngreso("nuevo_ingreso");

        Representante representante = new Representante();
        representante.setNombre("Luis");
        representante.setApellido("Pérez");
        representante.setCedula("V-87654321");
        representante.setTelefono("04121234567");

        List<String> columnas = List.of(
                "nombre", "cedula", "fechaNacimiento", "edad", "sexo", "nivelGradoSeccion",
                "tipoIngreso", "representanteNombre", "representanteCedula", "representanteTelefono");
        byte[] pdf = generator.generar((short) 2026, List.of(matricula), Map.of(7, representante), columnas);
        PdfReader reader = new PdfReader(pdf);
        String contenido = texto(reader).replaceAll("\\s+", " ");

        assertTrue(reader.getPageSizeWithRotation(1).getWidth()
                > reader.getPageSizeWithRotation(1).getHeight());
        assertTrue(contenido.contains("República Bolivariana de Venezuela"));
        assertTrue(contenido.contains("Ministerio del Poder Popular para la Educación"));
        assertTrue(contenido.contains("Cod. Plantel: OD34942317"));
        assertTrue(contenido.contains("NÓMINA ESTUDIANTIL"));
        assertTrue(contenido.contains("Nombre y apellido"));
        assertTrue(contenido.contains("V-12345678"));
        assertTrue(contenido.contains("28/02/2010"));
        assertTrue(contenido.contains("16"));
        assertTrue(contenido.contains("Media General · 3° grado · Sección A"));
        assertTrue(contenido.contains("Nuevo ingreso"));
        assertTrue(contenido.contains("Luis Pérez"));
        assertTrue(contenido.contains("V-87654321"));
        assertTrue(contenido.contains("04121234567"));
        reader.close();

        byte[] pdfReducido = generator.generar((short) 2026, List.of(matricula), Map.of(), List.of("nombre"));
        PdfReader readerReducido = new PdfReader(pdfReducido);
        String contenidoReducido = texto(readerReducido);
        assertTrue(contenidoReducido.contains("Nombre y apellido"));
        assertFalse(contenidoReducido.contains("Cédula"));
        readerReducido.close();
    }

    private String texto(PdfReader reader) throws Exception {
        StringBuilder contenido = new StringBuilder();
        for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
            contenido.append(new PdfTextExtractor(reader).getTextFromPage(pagina)).append(' ');
        }
        return contenido.toString();
    }
}
