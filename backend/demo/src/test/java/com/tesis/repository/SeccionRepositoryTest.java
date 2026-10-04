package com.tesis.repository;

import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.Seccion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeccionRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SeccionRepository seccionRepository;

    @Test
    void calculaCuposDisponiblesConLosCincoEstadosDeMatricula() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Primaria prueba");
        nivel.setOrdinal((short) 90);
        entityManager.persist(nivel);

        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        entityManager.persist(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion("Z");
        seccion.setCapacidadMaxima((short) 7);
        seccion.setAnioEscolar((short) 2026);
        entityManager.persist(seccion);

        persistirMatricula(seccion, "CUPOS-PREINSCRITO", "preinscrito");
        persistirMatricula(seccion, "CUPOS-EN-PROCESO", "en_proceso");
        persistirMatricula(seccion, "CUPOS-COMPLETADA", "completada");
        persistirMatricula(seccion, "CUPOS-RETIRADA", "retirada");
        persistirMatricula(seccion, "CUPOS-ANULADA", "anulada");
        entityManager.flush();

        Long ocupadas = entityManager.createQuery("select count(m) from Matricula m "
                + "where m.seccion = :seccion and m.anioEscolar = :anio "
                + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada')", Long.class)
            .setParameter("seccion", seccion)
            .setParameter("anio", (short) 2026)
            .getSingleResult();
        entityManager.refresh(seccion);
        assertEquals((short) 7, seccion.getCapacidadMaxima());
        assertEquals(3L, ocupadas);
        assertEquals(4L, seccionRepository.calcularCuposDisponibles(seccion.getId()).orElseThrow());
    }

    private void persistirMatricula(Seccion seccion, String cedula, String estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Prueba");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("M");
        entityManager.persist(estudiante);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setFechaSolicitud(LocalDate.of(2026, 1, 1));
        matricula.setEstadoMatricula(estado);
        entityManager.persist(matricula);
    }
}