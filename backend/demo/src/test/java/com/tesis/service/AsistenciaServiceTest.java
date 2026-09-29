package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AsistenciaHoyDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Asistencia;
import com.tesis.entity.CredencialQr;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.AsistenciaRepository;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

        private static final String QR_TOKEN = "signed-qr-token";
        private static final String QR_CREDENTIAL_ID = UUID.randomUUID().toString();

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @Mock
    private AsignacionTurnoRepository asignacionTurnoRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

        @Mock
        private CredencialQrRepository credencialQrRepository;

        @Mock
        private JwtProvider jwtProvider;

    private AsistenciaService asistenciaService;

    @BeforeEach
    void setUp() {
        asistenciaService = new AsistenciaService(
                asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider);
    }

    @Test
    void primerEscaneoRegistraEntradaYTardanza() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now()))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarEscaneo(QR_TOKEN);

        assertEquals("entrada", respuesta.getTipoRegistro());
        assertEquals("tardanza", respuesta.getEstado());
        verify(asistenciaRepository).save(any(Asistencia.class));
    }

    @Test
    void segundoEscaneoRegistraSalidaSinCambiarEntrada() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        LocalTime entrada = LocalTime.now().minusMinutes(1);
        asistencia.setHoraEntrada(entrada);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now()))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now()))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarEscaneo(QR_TOKEN);

        assertEquals("salida", respuesta.getTipoRegistro());
        assertEquals(entrada, respuesta.getHoraEntrada());
        verify(asistenciaRepository).save(asistencia);
    }

    @Test
    void tercerEscaneoSeRechazaSinModificarAsistencia() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setHoraEntrada(LocalTime.now().minusHours(1));
        asistencia.setHoraSalida(LocalTime.now().minusMinutes(1));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now()))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now()))
                .thenReturn(Optional.of(asistencia));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> asistenciaService.registrarEscaneo(QR_TOKEN));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

    @Test
    void salidaAntesDelInicioDelTurnoSeRechaza() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        turno.setHoraEntrada(LocalTime.now().plusMinutes(5));
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setHoraEntrada(LocalTime.now().minusMinutes(1));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now()))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now()))
                .thenReturn(Optional.of(asistencia));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> asistenciaService.registrarEscaneo(QR_TOKEN));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

    @Test
    void credencialRevocadaNoPuedeRegistrarAsistencia() {
        when(jwtProvider.validarTokenQr(QR_TOKEN)).thenReturn(new JwtProvider.QrClaims(
                7, QR_CREDENTIAL_ID, Instant.now().plusSeconds(3600)));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleadoActivo()));
        when(credencialQrRepository.findByIdAndEmpleado_IdAndActivaTrueAndExpiraEnAfter(
                eq(QR_CREDENTIAL_ID), eq(7), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> asistenciaService.registrarEscaneo(QR_TOKEN));

        assertEquals(HttpStatus.UNAUTHORIZED, excepcion.getStatusCode());
        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

        @Test
        void listarHoyCalculaDuracionYConservaSalidaNula() {
                Empleado empleado = empleadoActivo();
                Asistencia asistencia = asistenciaExistente(empleado, turnoVigente());
                asistencia.setHoraEntrada(LocalTime.now().minusHours(2).minusMinutes(15));
                asistencia.setHoraSalida(null);
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(LocalDate.now()))
                                .thenReturn(List.of(asistencia));

                List<AsistenciaHoyDTO> respuesta = asistenciaService.listarHoy();

                assertEquals(1, respuesta.size());
                assertEquals("Ana", respuesta.get(0).getNombre());
                assertEquals("Pérez", respuesta.get(0).getApellido());
                assertEquals(null, respuesta.get(0).getHoraSalida());
                assertEquals("2:15", respuesta.get(0).getHorasTrabajadas());
                assertEquals(135, respuesta.get(0).getMinutosTrabajados());
        }

        @Test
        void listarHoyCalculaHorasConSalidaRegistrada() {
                Empleado empleado = empleadoActivo();
                Asistencia asistencia = asistenciaExistente(empleado, turnoVigente());
                asistencia.setHoraEntrada(LocalTime.of(8, 0));
                asistencia.setHoraSalida(LocalTime.of(16, 30));
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(LocalDate.now()))
                                .thenReturn(List.of(asistencia));

                AsistenciaHoyDTO respuesta = asistenciaService.listarHoy().getFirst();

                assertEquals("8:30", respuesta.getHorasTrabajadas());
                assertEquals(510, respuesta.getMinutosTrabajados());
        }

    private Empleado empleadoActivo() {
        Empleado empleado = new Empleado();
        empleado.setId(7);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setActivo(true);
        return empleado;
    }

    private void stubCredencialQr() {
        when(jwtProvider.validarTokenQr(QR_TOKEN)).thenReturn(new JwtProvider.QrClaims(
                7, QR_CREDENTIAL_ID, Instant.now().plusSeconds(3600)));
        CredencialQr credencial = new CredencialQr();
        credencial.setId(QR_CREDENTIAL_ID);
        credencial.setActiva(true);
        credencial.setExpiraEn(LocalDateTime.now().plusHours(1));
        when(credencialQrRepository.findByIdAndEmpleado_IdAndActivaTrueAndExpiraEnAfter(
                eq(QR_CREDENTIAL_ID), eq(7), any(LocalDateTime.class)))
                .thenReturn(Optional.of(credencial));
    }

    private Turno turnoVigente() {
        Turno turno = new Turno();
        turno.setId(2);
        turno.setNombre("Diurno");
        turno.setHoraEntrada(LocalTime.now().minusHours(1));
        turno.setHoraSalida(LocalTime.now().plusHours(1));
        turno.setToleranciaMin((short) 0);
        turno.setActivo(true);
        turno.setLunes(true);
        turno.setMartes(true);
        turno.setMiercoles(true);
        turno.setJueves(true);
        turno.setViernes(true);
        turno.setSabado(true);
        turno.setDomingo(true);
        return turno;
    }

    private AsignacionTurno asignacion(Turno turno) {
        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setFechaDesde(LocalDate.now());
        asignacion.setTurno(turno);
        return asignacion;
    }

    private Asistencia asistenciaExistente(Empleado empleado, Turno turno) {
        Asistencia asistencia = new Asistencia();
        asistencia.setId(11);
        asistencia.setPersonal(empleado);
        asistencia.setTurno(turno);
        asistencia.setFecha(LocalDate.now());
        asistencia.setEstado("presente");
        asistencia.setMinutosTardanza((short) 0);
        asistencia.setMinutosExtra((short) 0);
        asistencia.setCorregido(false);
        asistencia.setCreadoEn(LocalDateTime.now());
        return asistencia;
    }
}