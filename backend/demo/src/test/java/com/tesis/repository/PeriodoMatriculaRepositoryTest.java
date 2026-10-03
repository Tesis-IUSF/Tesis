package com.tesis.repository;

import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
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
class PeriodoMatriculaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PeriodoMatriculaRepository periodoRepository;

    @Test
    void soloDevuelvePeriodosActivosDelTipoYAnioConsultados() {
        PeriodoMatricula inactivo = crearPeriodo(false, TipoPeriodoMatricula.preinscripcion);
        PeriodoMatricula activo = crearPeriodo(true, TipoPeriodoMatricula.preinscripcion);
        crearPeriodo(true, TipoPeriodoMatricula.inscripcion);
        entityManager.flush();

        var periodos = periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                (short) 2026, TipoPeriodoMatricula.preinscripcion);

        assertEquals(1, periodos.size());
        assertEquals(activo.getId(), periodos.getFirst().getId());
        org.junit.jupiter.api.Assertions.assertNotEquals(inactivo.getId(), periodos.getFirst().getId());
    }

    private PeriodoMatricula crearPeriodo(boolean activo, TipoPeriodoMatricula tipo) {
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setAnioEscolar((short) 2026);
        periodo.setTipo(tipo);
        periodo.setFechaInicio(LocalDate.of(2026, 9, 9));
        periodo.setFechaFin(LocalDate.of(2026, 10, 31));
        periodo.setActivo(activo);
        entityManager.persist(periodo);
        return periodo;
    }
}