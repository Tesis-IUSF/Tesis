package com.tesis.service;

import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.dto.GestionCalendarioDTO.AnioEscolarRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.PeriodoRequestDTO;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.MatriculaRepository;
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
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarioMatriculaServiceTest {

    private static final Short ANIO = (short) 2026;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 3);

    private final AnioEscolarRepository anioRepository = mock(AnioEscolarRepository.class);
    private final PeriodoMatriculaRepository periodoRepository = mock(PeriodoMatriculaRepository.class);
    private final MatriculaRepository matriculaRepository = mock(MatriculaRepository.class);
    private final CalendarioMatriculaService service = new CalendarioMatriculaService(
            anioRepository, periodoRepository, matriculaRepository,
            Clock.fixed(Instant.parse("2026-10-03T16:00:00Z"), ZoneId.of("America/Caracas")));

    @BeforeEach
    void configurarAnio() {
        when(anioRepository.existsByAnio(ANIO)).thenReturn(true);
        AnioEscolar anio = new AnioEscolar();
        anio.setAnio(ANIO);
        anio.setNombre("2026-2027");
        anio.setActivo(true);
        when(anioRepository.findByAnio(ANIO)).thenReturn(Optional.of(anio));
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

    @Test
    void nuevasPreinscripcionesSoloPuedenUsarElAnioActivo() {
        AnioEscolar anterior = new AnioEscolar();
        anterior.setAnio((short) 2025);
        anterior.setActivo(false);
        when(anioRepository.findByAnio((short) 2025)).thenReturn(Optional.of(anterior));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarPreinscripcion((short) 2025));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void permiteContinuarTramiteAnteriorEnVentanaExtemporanea() {
        AnioEscolar anterior = new AnioEscolar();
        anterior.setAnio((short) 2025);
        anterior.setActivo(false);
        when(anioRepository.findByAnio((short) 2025)).thenReturn(Optional.of(anterior));
        when(periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(
                (short) 2025, TipoPeriodoMatricula.extemporanea))
                .thenReturn(List.of(periodo(TipoPeriodoMatricula.extemporanea, HOY, HOY)));

        service.validarGestionInscripcion((short) 2025, "en_proceso");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validarGestionInscripcion((short) 2025, "preinscrito"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void crearAnioCopiaPeriodosDelAnteriorConFechasDesplazadas() {
        Short anioNuevo = 2027;
        AnioEscolar anterior = new AnioEscolar();
        anterior.setAnio(ANIO);
        when(anioRepository.existsByAnio(anioNuevo)).thenReturn(false);
        when(anioRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(AnioEscolar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(anioRepository.findTopByAnioLessThanOrderByAnioDesc(anioNuevo))
                .thenReturn(Optional.of(anterior));
        when(periodoRepository.findByAnioEscolarAndActivoTrue(ANIO))
                .thenReturn(List.of(
                        periodo(TipoPeriodoMatricula.preinscripcion,
                                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1)),
                        periodo(TipoPeriodoMatricula.inscripcion,
                                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)),
                        periodo(TipoPeriodoMatricula.extemporanea,
                                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 15))));
        List<PeriodoMatricula> guardados = new ArrayList<>();
        when(periodoRepository.saveAll(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Iterable<PeriodoMatricula> periodos = invocation.getArgument(0);
                    periodos.forEach(guardados::add);
                    return guardados;
                });
        when(periodoRepository.findByAnioEscolarOrderByFechaInicioAsc(anioNuevo))
                .thenAnswer(invocation -> guardados);

        var respuesta = service.crearAnio(new AnioEscolarRequestDTO(
                anioNuevo, "2027-2028", LocalDate.of(2027, 9, 1), LocalDate.of(2028, 7, 15)));

        assertEquals(ANIO, respuesta.getSugeridoDesdeAnio());
        assertEquals(3, respuesta.getPeriodos().size());
        assertEquals(LocalDate.of(2027, 5, 1), respuesta.getPeriodos().getFirst().getFechaInicio());
        assertEquals(false, respuesta.isActivo());
    }

    @Test
    void rechazaPeriodosDelMismoTipoQueSeSolapan() {
        PeriodoMatricula existente = periodo(TipoPeriodoMatricula.preinscripcion,
                HOY, HOY.plusDays(3));
        existente.setId(8);
        when(periodoRepository.findByAnioEscolarAndTipo(ANIO, TipoPeriodoMatricula.preinscripcion))
                .thenReturn(List.of(existente));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crearPeriodo(ANIO, new PeriodoRequestDTO(
                        TipoPeriodoMatricula.preinscripcion,
                        HOY.plusDays(3), HOY.plusDays(5))));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void permiteSolapamientoEntrePeriodosDeTiposDistintos() {
        when(periodoRepository.findByAnioEscolarAndTipo(ANIO, TipoPeriodoMatricula.inscripcion))
                .thenReturn(List.of());
        when(periodoRepository.save(org.mockito.ArgumentMatchers.any(PeriodoMatricula.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.crearPeriodo(ANIO, new PeriodoRequestDTO(
                TipoPeriodoMatricula.inscripcion, HOY, HOY.plusDays(2)));

        assertEquals(TipoPeriodoMatricula.inscripcion, resultado.getTipo());
    }

    @Test
    void activarAnioCierraPreinscripcionEInscripcionAnterioresYConservaExtemporanea() {
        Short anioNuevo = 2027;
        AnioEscolar nuevo = new AnioEscolar();
        nuevo.setAnio(anioNuevo);
        nuevo.setNombre("2027-2028");
        nuevo.setFechaInicio(LocalDate.of(2027, 9, 1));
        nuevo.setFechaFin(LocalDate.of(2028, 7, 15));
        AnioEscolar anterior = new AnioEscolar();
        anterior.setAnio(ANIO);
        anterior.setActivo(true);
        when(anioRepository.findAllForUpdate()).thenReturn(List.of(anterior, nuevo));
        List<PeriodoMatricula> periodosNuevos = List.of(
                periodoConId(1, anioNuevo, TipoPeriodoMatricula.preinscripcion),
                periodoConId(2, anioNuevo, TipoPeriodoMatricula.inscripcion),
                periodoConId(3, anioNuevo, TipoPeriodoMatricula.extemporanea));
        when(periodoRepository.findByAnioEscolarOrderByFechaInicioAsc(anioNuevo))
                .thenReturn(periodosNuevos);
        List<PeriodoMatricula> periodosAnteriores = List.of(
                periodoConId(4, ANIO, TipoPeriodoMatricula.preinscripcion),
                periodoConId(5, ANIO, TipoPeriodoMatricula.inscripcion),
                periodoConId(6, ANIO, TipoPeriodoMatricula.extemporanea));
        when(periodoRepository.findByAnioEscolarAndActivoTrue(ANIO))
                .thenReturn(periodosAnteriores);
        when(periodoRepository.findByAnioEscolarAndTipo(anioNuevo,
                TipoPeriodoMatricula.preinscripcion)).thenReturn(List.of());
        when(periodoRepository.findByAnioEscolarAndTipo(anioNuevo,
                TipoPeriodoMatricula.inscripcion)).thenReturn(List.of());
        when(periodoRepository.findByAnioEscolarAndTipo(anioNuevo,
                TipoPeriodoMatricula.extemporanea)).thenReturn(List.of());

        var respuesta = service.activarAnio(anioNuevo);

        assertEquals(true, respuesta.isActivo());
        assertEquals(false, anterior.getActivo());
        assertEquals(false, periodosAnteriores.get(0).getActivo());
        assertEquals(false, periodosAnteriores.get(1).getActivo());
        assertEquals(true, periodosAnteriores.get(2).getActivo());
    }

    @Test
    void noActivaAnioSinLosTresTiposDePeriodo() {
        Short anioNuevo = 2027;
        AnioEscolar nuevo = new AnioEscolar();
        nuevo.setAnio(anioNuevo);
        nuevo.setNombre("2027-2028");
        nuevo.setFechaInicio(LocalDate.of(2027, 9, 1));
        nuevo.setFechaFin(LocalDate.of(2028, 7, 15));
        when(anioRepository.findAllForUpdate()).thenReturn(List.of(nuevo));
        when(periodoRepository.findByAnioEscolarOrderByFechaInicioAsc(anioNuevo))
                .thenReturn(List.of(periodoConId(1, anioNuevo, TipoPeriodoMatricula.preinscripcion)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activarAnio(anioNuevo));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void soloPermiteBorrarPeriodoNoUsadoDeAnioFuturoEnPreparacion() {
        Short futuro = 2027;
        AnioEscolar preparado = new AnioEscolar();
        preparado.setAnio(futuro);
        preparado.setFechaInicio(HOY.plusMonths(11));
        preparado.setActivo(false);
        PeriodoMatricula periodo = periodoConId(77, futuro, TipoPeriodoMatricula.preinscripcion);
        when(periodoRepository.findById(77)).thenReturn(Optional.of(periodo));
        when(anioRepository.findByAnio(futuro)).thenReturn(Optional.of(preparado));
        when(matriculaRepository.existsByAnioEscolar(futuro)).thenReturn(false);

        service.eliminarPeriodoEnPreparacion(77);

        org.mockito.Mockito.verify(periodoRepository).delete(periodo);
    }

    @Test
    void noPermiteBorrarPeriodoDeAnioConMatriculas() {
        Short futuro = 2027;
        AnioEscolar preparado = new AnioEscolar();
        preparado.setAnio(futuro);
        preparado.setFechaInicio(HOY.plusMonths(11));
        preparado.setActivo(false);
        PeriodoMatricula periodo = periodoConId(78, futuro, TipoPeriodoMatricula.preinscripcion);
        when(periodoRepository.findById(78)).thenReturn(Optional.of(periodo));
        when(anioRepository.findByAnio(futuro)).thenReturn(Optional.of(preparado));
        when(matriculaRepository.existsByAnioEscolar(futuro)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminarPeriodoEnPreparacion(78));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void devuelveCalendarioVacioConMensajeCuandoNoHayAnioActivo() {
        when(anioRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());

        var calendario = service.obtenerCalendario(null);

        assertNull(calendario.getAnioActivo());
        assertNull(calendario.getAnioConsultado());
        assertEquals(List.of(), calendario.getPeriodos());
        assertEquals("No hay un año escolar activo configurado", calendario.getMensaje());
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

    private PeriodoMatricula periodoConId(Integer id, Short anio, TipoPeriodoMatricula tipo) {
        PeriodoMatricula periodo = periodo(tipo, HOY.minusDays(1), HOY.plusDays(1));
        periodo.setId(id);
        periodo.setAnioEscolar(anio);
        return periodo;
    }

}