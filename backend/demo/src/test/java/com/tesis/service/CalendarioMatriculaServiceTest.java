package com.tesis.service;

import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.PeriodoMatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarioMatriculaServiceTest {

    private static final Short ANIO = (short) 2026;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 3);

    private final AnioEscolarRepository anioRepository = mock(AnioEscolarRepository.class);
    private final PeriodoMatriculaRepository periodoRepository = mock(PeriodoMatriculaRepository.class);
    private final CalendarioMatriculaService service = new CalendarioMatriculaService(
            anioRepository, periodoRepository,
            Clock.fixed(Instant.parse("2026-10-03T16:00:00Z"), ZoneId.of("America/Caracas")));

    @BeforeEach
    void configurarAnio() {
        when(anioRepository.existsByAnio(ANIO)).thenReturn(true);
    }

    @Test
    void aceptaLimitesInclusivosYRechazaElDiaAnteriorYPosterior() {
        PeriodoMatricula limiteInicio = periodo(TipoPeriodoMatricula.preinscripcion,
                HOY, HOY.plusDays(2));
        PeriodoMatricula limiteFin = periodo(TipoPeriodoMatricula.preinscripcion,
                HOY.minusDays(2), HOY);
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.preinscripcion))
                .thenReturn(List.of(limiteInicio), List.of(limiteFin),
                        List.of(periodo(TipoPeriodoMatricula.preinscripcion,
                                HOY.plusDays(1), HOY.plusDays(2))),
                        List.of(periodo(TipoPeriodoMatricula.preinscripcion,
                                HOY.minusDays(2), HOY.minusDays(1))));

        service.validarPreinscripcion(ANIO);
        service.validarPreinscripcion(ANIO);
        assertThrows(ResponseStatusException.class, () -> service.validarPreinscripcion(ANIO));
        assertThrows(ResponseStatusException.class, () -> service.validarPreinscripcion(ANIO));
    }

    @Test
    void requierePeriodoActivoDelTipoSolicitadoAunqueHayaOtroTipoAbierto() {
        PeriodoMatricula inscripcion = periodo(TipoPeriodoMatricula.inscripcion,
                HOY.minusDays(1), HOY.plusDays(1));
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.preinscripcion)).thenReturn(List.of());
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.inscripcion)).thenReturn(List.of(inscripcion));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarPreinscripcion(ANIO));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void rechazaAusenciaDePeriodoYDistingueUnPeriodoInactivo() {
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.preinscripcion)).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarPreinscripcion(ANIO));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void extemporaneaSoloPermiteContinuarUnaMatriculaEnProceso() {
        PeriodoMatricula extemporanea = periodo(TipoPeriodoMatricula.extemporanea,
                HOY, HOY);
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.inscripcion)).thenReturn(List.of());
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                ANIO, TipoPeriodoMatricula.extemporanea)).thenReturn(List.of(extemporanea));

        service.validarGestionInscripcion(ANIO, "en_proceso");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarGestionInscripcion(ANIO, "preinscrito"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private PeriodoMatricula periodo(TipoPeriodoMatricula tipo,
                                    LocalDate inicio,
                                    LocalDate fin) {
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setAnioEscolar(ANIO);
        periodo.setTipo(tipo);
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        periodo.setActivo(true);
        return periodo;
    }

}