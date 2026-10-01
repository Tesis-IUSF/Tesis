package com.tesis.service;

import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RetiroMatricula;
import com.tesis.entity.Seccion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConstanciaServiceTest {

    @Autowired
    private ConstanciaService constanciaService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void generaPdfDeInscripcionParaMatriculaFormalizada() {
        Matricula matricula = crearMatricula("completada", "CONSTANCIA-INS-01", "A");
        matricula.setFechaFormalizacion(LocalDate.of(2026, 1, 15));
        entityManager.flush();

        byte[] pdf = constanciaService.generarConstanciaInscripcion(matricula.getId());

        assertTrue(new String(pdf, 0, 5).startsWith("%PDF-"));
    }

    @Test
    void rechazaConstanciaDeInscripcionNoFormalizada() {
        Matricula matricula = crearMatricula("en_proceso", "CONSTANCIA-PEND-01", "B");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> constanciaService.generarConstanciaInscripcion(matricula.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void generaConstanciaDeAsignacionParaMatriculaPreinscrita() {
        Matricula matricula = crearMatricula("preinscrito", "CONSTANCIA-CUPO-01", "E");

        byte[] pdf = constanciaService.generarConstanciaAsignacionCupo(matricula.getId());

        assertTrue(new String(pdf, 0, 5).startsWith("%PDF-"));
        assertTrue(extraerTexto(pdf).contains("2026-2027"));
    }

    @Test
    void rechazaConstanciaDeAsignacionCuandoElCupoYaNoEstaReservado() {
        Matricula matricula = crearMatricula("completada", "CONSTANCIA-CUPO-02", "F");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> constanciaService.generarConstanciaAsignacionCupo(matricula.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void generaPdfDeRetiroYValidaQueCorrespondaALaMatricula() {
        Matricula matricula = crearMatricula("retirada", "CONSTANCIA-RET-01", "C");
        RetiroMatricula retiro = new RetiroMatricula();
        retiro.setMatricula(matricula);
        retiro.setSolicitanteNombre("Representante");
        retiro.setSolicitanteApellido("Prueba");
        retiro.setSolicitanteCedula("CED-RET-01");
        retiro.setMotivo("Cambio de domicilio");
        retiro.setFechaRetiro(LocalDate.of(2026, 2, 1));
        entityManager.persist(retiro);
        entityManager.flush();

        byte[] pdf = constanciaService.generarConstanciaRetiro(matricula.getId(), retiro.getId());
        Matricula otraMatricula = crearMatricula("retirada", "CONSTANCIA-RET-02", "D");

        assertTrue(new String(pdf, 0, 5).startsWith("%PDF-"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> constanciaService.generarConstanciaRetiro(otraMatricula.getId(), retiro.getId()));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Matricula crearMatricula(String estado, String cedula, String letra) {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Nivel constancia " + letra);
        nivel.setOrdinal((short) (130 + letra.charAt(0)));
        entityManager.persist(nivel);

        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        entityManager.persist(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion(letra);
        seccion.setAnioEscolar((short) 2026);
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Constancia");
        estudiante.setFechaNacimiento(LocalDate.of(2017, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setFechaSolicitud(LocalDate.of(2026, 1, 1));
        matricula.setEstadoMatricula(estado);
        entityManager.persist(matricula);
        entityManager.flush();
        return matricula;
    }

    private String extraerTexto(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            reader.close();
            return texto;
        } catch (Exception exception) {
            throw new AssertionError("No se pudo leer el texto del PDF", exception);
        }
    }
}