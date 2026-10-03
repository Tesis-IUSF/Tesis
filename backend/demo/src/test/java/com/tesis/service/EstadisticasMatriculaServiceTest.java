package com.tesis.service;

import com.tesis.dto.EstadisticasMatriculaDTO;
import com.tesis.entity.NivelEducativo;
import com.tesis.repository.EstadisticasRepository;
import com.tesis.repository.NivelEducativoRepository;
import com.tesis.repository.SeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticasMatriculaServiceTest {

    @Mock
    private EstadisticasRepository estadisticasRepository;
    @Mock
    private SeccionRepository seccionRepository;
    @Mock
    private NivelEducativoRepository nivelEducativoRepository;
        @Mock
        private EstadisticasPdfGenerator estadisticasPdfGenerator;

    private EstadisticasMatriculaService service;

    @BeforeEach
    void setUp() {
        service = new EstadisticasMatriculaService(
                estadisticasRepository, seccionRepository, nivelEducativoRepository,
                estadisticasPdfGenerator);
    }

    @Test
    void obtieneEstadisticasGeneralesYCalculaPorcentaje() {
        prepararEstadisticas(null, null, 7L, 13L, 20L, 4L, 3L,
                List.<Object[]>of(new Object[] {"F", 8L}, new Object[] {"M", 5L}));

        EstadisticasMatriculaDTO resultado = service.obtenerEstadisticasGenerales();

        assertEquals(7L, resultado.getCuposDisponibles());
        assertEquals(13L, resultado.getCuposOcupados());
        assertEquals(65.0, resultado.getPorcentajeOcupacion());
        assertEquals(8L, resultado.getEstudiantesPorSexo().get("F"));
        assertEquals(5L, resultado.getEstudiantesPorSexo().get("M"));
        assertEquals(4L, resultado.getNuevoIngreso());
        assertEquals(3L, resultado.getRegularesPendientes());
    }

    @Test
    void porcentajeEsCeroCuandoNoHayCapacidad() {
        prepararEstadisticas(null, null, 0L, 0L, 0L, 0L, 0L, List.of());

        EstadisticasMatriculaDTO resultado = service.obtenerEstadisticasGenerales();

        assertEquals(0.0, resultado.getPorcentajeOcupacion());
        assertEquals(0L, resultado.getCuposDisponibles());
    }

    @Test
    void obtieneEstadisticasDeLaSeccionSolicitada() {
        when(seccionRepository.existsById(12)).thenReturn(true);
        prepararEstadisticas(12, null, 1L, 3L, 4L, 1L, 2L,
                List.<Object[]>of(new Object[] {"F", 3L}));

        EstadisticasMatriculaDTO resultado = service.obtenerEstadisticasSeccion(12);

        assertEquals(75.0, resultado.getPorcentajeOcupacion());
        assertEquals(3L, resultado.getEstudiantesPorSexo().get("F"));
        verify(estadisticasRepository).obtenerCuposDisponibles(12, null);
    }

    @Test
    void rechazaSeccionInexistenteSinConsultarEstadisticas() {
        when(seccionRepository.existsById(404)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.obtenerEstadisticasSeccion(404));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(estadisticasRepository, never()).obtenerCuposDisponibles(404, null);
    }

    @Test
    void buscaNivelSinDistinguirMayusculasYUsaNombreCanonico() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Educación Primaria");
        when(nivelEducativoRepository.findByNombreIgnoreCase("educación primaria"))
                .thenReturn(Optional.of(nivel));
        prepararEstadisticas(null, "Educación Primaria", 5L, 5L, 10L, 2L, 1L,
                List.<Object[]>of(new Object[] {"F", 3L}, new Object[] {"M", 2L}));

        EstadisticasMatriculaDTO resultado = service.obtenerEstadisticasNivel("educación primaria");

        assertEquals(50.0, resultado.getPorcentajeOcupacion());
        assertEquals(2L, resultado.getNuevoIngreso());
        verify(estadisticasRepository).obtenerCuposOcupados(null, "Educación Primaria");
    }

    @Test
    void agrupaEstudiantesPorSeccionYConvierteSexoAHembrasYVarones() {
        prepararEstadisticas(null, null, 5L, 5L, 10L, 2L, 1L, List.of());
        when(estadisticasRepository.obtenerEstudiantesPorSeccionYSexo(null, null)).thenReturn(List.of(
                new Object[] {"Inicial", (short) 0, (short) 1, "Grupo I", "A", 101, "F", 4L},
                new Object[] {"Inicial", (short) 0, (short) 1, "Grupo I", "A", 101, "M", 3L},
                new Object[] {"Media General", (short) 1, (short) 1, null, "B", 102, "Otro", 2L}));

        EstadisticasMatriculaDTO resultado = service.obtenerEstadisticasGenerales();

        assertEquals(2, resultado.getEstudiantesPorSeccion().size());
        assertEquals("Grupo I", resultado.getEstudiantesPorSeccion().getFirst().getSeccion());
        assertEquals(4L, resultado.getEstudiantesPorSeccion().getFirst()
            .getEstudiantesPorSexo().get("F"));
        assertEquals(3L, resultado.getEstudiantesPorSeccion().getFirst()
            .getEstudiantesPorSexo().get("M"));
        assertEquals(7L, resultado.getEstudiantesPorSeccion().getFirst().getTotal());
        assertEquals("1er año B", resultado.getEstudiantesPorSeccion().getLast().getSeccion());
        assertEquals(2L, resultado.getEstudiantesPorSeccion().getLast()
            .getEstudiantesPorSexo().get("Otro"));
    }

    private void prepararEstadisticas(Integer seccionId,
                                      String nivel,
                                      long disponibles,
                                      long ocupados,
                                      long capacidad,
                                      long nuevos,
                                      long regularesPendientes,
                                      List<Object[]> estudiantesPorSexo) {
        when(estadisticasRepository.obtenerCuposDisponibles(seccionId, nivel)).thenReturn(disponibles);
        when(estadisticasRepository.obtenerCuposOcupados(seccionId, nivel)).thenReturn(ocupados);
        when(estadisticasRepository.obtenerCapacidadTotal(seccionId, nivel)).thenReturn(capacidad);
        when(estadisticasRepository.obtenerEstudiantesPorSexo(seccionId, nivel))
                .thenReturn(estudiantesPorSexo);
        when(estadisticasRepository.obtenerNuevoIngreso(seccionId, nivel)).thenReturn(nuevos);
        when(estadisticasRepository.obtenerRegularesPendientes(seccionId, nivel))
                .thenReturn(regularesPendientes);
    }
}