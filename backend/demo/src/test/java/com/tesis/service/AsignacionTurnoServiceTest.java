package com.tesis.service;

import com.tesis.dto.AsignacionTurnoDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionTurnoServiceTest {

    @Mock
    private AsignacionTurnoRepository asignacionTurnoRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private TurnoRepository turnoRepository;

    private AsignacionTurnoService asignacionTurnoService;

    @BeforeEach
    void setUp() {
        asignacionTurnoService = new AsignacionTurnoService(
                asignacionTurnoRepository, empleadoRepository, turnoRepository);
    }

    @Test
    void asignarGuardaTurnoParaEmpleado() {
        LocalDate inicio = LocalDate.of(2026, 10, 5);
        Empleado empleado = new Empleado();
        empleado.setId(7);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        Turno turno = new Turno();
        turno.setId(2);
        turno.setNombre("Turno mañana administrativo");
        turno.setActivo(true);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findById(2)).thenReturn(Optional.of(turno));
        when(asignacionTurnoRepository.existeSolapamiento(7, inicio, null)).thenReturn(false);
        when(asignacionTurnoRepository.save(any())).thenAnswer(invocation -> {
            var asignacion = invocation.getArgument(0,
                    com.tesis.entity.AsignacionTurno.class);
            asignacion.setId(11);
            return asignacion;
        });

        AsignacionTurnoDTO.Response respuesta = asignacionTurnoService.asignar(
                new AsignacionTurnoDTO.Request(7, 2, inicio, null));

        assertEquals(11, respuesta.getId());
        assertEquals("Ana Pérez", respuesta.getEmpleadoNombre());
        assertEquals("Turno mañana administrativo", respuesta.getTurnoNombre());
        assertEquals(inicio, respuesta.getFechaDesde());
        verify(asignacionTurnoRepository).save(any());
    }

    @Test
    void asignarRechazaRangoInvertido() {
        LocalDate desde = LocalDate.of(2026, 10, 6);
        LocalDate hasta = desde.minusDays(1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> asignacionTurnoService.asignar(new AsignacionTurnoDTO.Request(7, 2, desde, hasta)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(empleadoRepository, never()).findByIdForUpdate(7);
    }

    @Test
    void asignarRechazaSolapamientoConAsignacionExistente() {
        LocalDate inicio = LocalDate.of(2026, 10, 5);
        Empleado empleado = new Empleado();
        empleado.setId(7);
        Turno turno = new Turno();
        turno.setId(2);
        turno.setActivo(true);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findById(2)).thenReturn(Optional.of(turno));
        when(asignacionTurnoRepository.existeSolapamiento(7, inicio, null)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> asignacionTurnoService.asignar(
                        new AsignacionTurnoDTO.Request(7, 2, inicio, null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(asignacionTurnoRepository, never()).save(any());
    }

        @Test
        void reemplazarCierraAsignacionAnteriorElDiaPrevio() {
        LocalDate inicioAnterior = LocalDate.of(2026, 1, 1);
        LocalDate fechaCambio = LocalDate.of(2026, 10, 12);
        Empleado empleado = empleado(7);
        Turno turnoAnterior = turno(2, "Turno mañana administrativo");
        Turno nuevoTurno = turno(3, "Turno tarde administrativo");
        AsignacionTurno vigente = asignacion(10, empleado, turnoAnterior, inicioAnterior, null);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findById(3)).thenReturn(Optional.of(nuevoTurno));
        when(asignacionTurnoRepository.buscarVigentesEnFecha(7, fechaCambio))
            .thenReturn(List.of(vigente));
        when(asignacionTurnoRepository.existeSolapamientoExcepto(7, fechaCambio, null, 10))
            .thenReturn(false);
        when(asignacionTurnoRepository.save(any())).thenAnswer(invocation -> {
            AsignacionTurno asignacion = invocation.getArgument(0);
            if (asignacion.getId() == null) {
            asignacion.setId(11);
            }
            return asignacion;
        });

        AsignacionTurnoDTO.Response respuesta = asignacionTurnoService.reemplazarVigente(
            7, new AsignacionTurnoDTO.CambioRequest(3, fechaCambio));

        assertEquals(fechaCambio.minusDays(1), vigente.getFechaHasta());
        assertEquals(11, respuesta.getId());
        assertEquals(3, respuesta.getTurnoId());
        assertEquals(fechaCambio, respuesta.getFechaDesde());
        assertEquals(null, respuesta.getFechaHasta());
        verify(asignacionTurnoRepository, org.mockito.Mockito.times(2)).save(any());
        }

        @Test
        void reemplazarEnFechaInicialActualizaLaAsignacionExistente() {
        LocalDate fechaCambio = LocalDate.of(2026, 10, 12);
        Empleado empleado = empleado(7);
        Turno nuevoTurno = turno(3, "Turno tarde administrativo");
        AsignacionTurno vigente = asignacion(10, empleado,
            turno(2, "Turno mañana administrativo"), fechaCambio, null);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findById(3)).thenReturn(Optional.of(nuevoTurno));
        when(asignacionTurnoRepository.buscarVigentesEnFecha(7, fechaCambio))
            .thenReturn(List.of(vigente));
        when(asignacionTurnoRepository.existeSolapamientoExcepto(7, fechaCambio, null, 10))
            .thenReturn(false);
        when(asignacionTurnoRepository.save(vigente)).thenReturn(vigente);

        AsignacionTurnoDTO.Response respuesta = asignacionTurnoService.reemplazarVigente(
            7, new AsignacionTurnoDTO.CambioRequest(3, fechaCambio));

        assertEquals(3, respuesta.getTurnoId());
        assertEquals(fechaCambio, respuesta.getFechaDesde());
        verify(asignacionTurnoRepository).save(vigente);
        }

        @Test
        void reemplazarRechazaOtraAsignacionFuturaSolapadaSinCerrarLaVigente() {
        LocalDate inicioAnterior = LocalDate.of(2026, 1, 1);
        LocalDate fechaCambio = LocalDate.of(2026, 10, 12);
        Empleado empleado = empleado(7);
        AsignacionTurno vigente = asignacion(10, empleado,
            turno(2, "Turno mañana administrativo"), inicioAnterior, null);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findById(3)).thenReturn(Optional.of(turno(3, "Turno tarde administrativo")));
        when(asignacionTurnoRepository.buscarVigentesEnFecha(7, fechaCambio))
            .thenReturn(List.of(vigente));
        when(asignacionTurnoRepository.existeSolapamientoExcepto(7, fechaCambio, null, 10))
            .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> asignacionTurnoService.reemplazarVigente(
                7, new AsignacionTurnoDTO.CambioRequest(3, fechaCambio)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(null, vigente.getFechaHasta());
        verify(asignacionTurnoRepository, never()).save(any());
        }

        private Empleado empleado(Integer id) {
        Empleado empleado = new Empleado();
        empleado.setId(id);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        return empleado;
        }

        private Turno turno(Integer id, String nombre) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setNombre(nombre);
        turno.setActivo(true);
        return turno;
        }

        private AsignacionTurno asignacion(Integer id, Empleado empleado, Turno turno,
                           LocalDate desde, LocalDate hasta) {
        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setId(id);
        asignacion.setPersonal(empleado);
        asignacion.setTurno(turno);
        asignacion.setFechaDesde(desde);
        asignacion.setFechaHasta(hasta);
        return asignacion;
        }
}