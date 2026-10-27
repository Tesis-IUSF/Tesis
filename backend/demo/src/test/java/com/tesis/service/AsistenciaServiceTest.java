package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AsistenciaHoyDTO;
import com.tesis.dto.AsistenciaDTO.AusenciaDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Asistencia;
import com.tesis.entity.Cargo;
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
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        private Clock clock;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.systemDefault();
        clock = Clock.fixed(LocalDate.now().atTime(1, 0).atZone(zone).toInstant(), zone);
        asistenciaService = new AsistenciaService(
                asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider, clock);
    }

    @Test
    void primerEscaneoRegistraEntradaYTardanza() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now().minusDays(1)))
                .thenReturn(List.of());
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
        LocalTime entrada = LocalTime.now(clock).minusMinutes(1);
        asistencia.setHoraEntrada(entrada);
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now(clock).minusDays(1)))
                .thenReturn(List.of());
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
    void escaneoDeMadrugadaRegistraEntradaEnLaFechaDelTurnoNocturno() {
        LocalDate fechaTurno = LocalDate.now(clock);
        fijarAhora(fechaTurno.plusDays(1).atTime(1, 0));
        Empleado empleado = empleadoActivo();
        Turno turno = turnoNocturno();
        stubCredencialQr();
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, fechaTurno))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, fechaTurno))
                .thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarEscaneo(QR_TOKEN);

        assertEquals(fechaTurno, respuesta.getFecha());
        assertEquals(LocalTime.of(1, 0), respuesta.getHoraEntrada());
        assertEquals("tardanza", respuesta.getEstado());
        assertEquals(Short.valueOf((short) 180), respuesta.getMinutosTardanza());
    }

    @Test
    void escaneoDeMadrugadaCierraLaEntradaAbiertaDelTurnoAnterior() {
        LocalDate fechaTurno = LocalDate.now(clock);
        fijarAhora(fechaTurno.plusDays(1).atTime(1, 0));
        Empleado empleado = empleadoActivo();
        Turno turno = turnoNocturno();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setFecha(fechaTurno);
        asistencia.setHoraEntrada(LocalTime.of(22, 0));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, fechaTurno))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, fechaTurno))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarEscaneo(QR_TOKEN);

        assertEquals(fechaTurno, respuesta.getFecha());
        assertEquals(LocalTime.of(1, 0), respuesta.getHoraSalida());
        assertEquals("salida_anticipada", respuesta.getEstado());
        verify(asistenciaRepository).save(asistencia);
    }

    @Test
    void escaneoDeMadrugadaRechazaTurnoAnteriorYaCompletado() {
        LocalDate fechaTurno = LocalDate.now(clock);
        fijarAhora(fechaTurno.plusDays(1).atTime(1, 0));
        Empleado empleado = empleadoActivo();
        Turno turno = turnoNocturno();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setFecha(fechaTurno);
        asistencia.setHoraEntrada(LocalTime.of(22, 0));
        asistencia.setHoraSalida(LocalTime.of(0, 30));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, fechaTurno))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, fechaTurno))
                .thenReturn(Optional.of(asistencia));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> asistenciaService.registrarEscaneo(QR_TOKEN));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

    @Test
    void escaneoDespuesDelFinDelTurnoCierraEntradaNocturnaAbierta() {
        LocalDate fechaTurno = LocalDate.now(clock);
        fijarAhora(fechaTurno.plusDays(1).atTime(7, 0));
        Empleado empleado = empleadoActivo();
        Turno turno = turnoNocturno();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setFecha(fechaTurno);
        asistencia.setHoraEntrada(LocalTime.of(22, 0));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, fechaTurno))
                .thenReturn(List.of(asignacion(turno)));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, fechaTurno))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarEscaneo(QR_TOKEN);

        assertEquals(fechaTurno, respuesta.getFecha());
        assertEquals(LocalTime.of(7, 0), respuesta.getHoraSalida());
        assertEquals(Short.valueOf((short) 60), respuesta.getMinutosExtra());
    }

    @Test
    void salidaManualRegistraHoraYTipoSinQr() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setHoraEntrada(LocalTime.now(clock).minusMinutes(1));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now(clock).minusDays(1)))
                .thenReturn(Optional.empty());
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now(clock)))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarSalidaManual(7);

        assertEquals("salida", respuesta.getTipoRegistro());
        assertEquals(LocalTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
                respuesta.getHoraSalida());
        verify(asistenciaRepository).save(asistencia);
    }

    @Test
    void salidaManualSinEntradaSeRechaza() {
        Empleado empleado = empleadoActivo();
        Asistencia asistencia = asistenciaExistente(empleado, turnoVigente());
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now(clock).minusDays(1)))
                .thenReturn(Optional.empty());
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, LocalDate.now(clock)))
                .thenReturn(Optional.of(asistencia));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> asistenciaService.registrarSalidaManual(7));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(asistenciaRepository, never()).save(any(Asistencia.class));
    }

    @Test
    void salidaManualCierraTurnoNocturnoAnteriorAunqueEmpleadoEsteInactivo() {
        LocalDate fechaTurno = LocalDate.now(clock);
        fijarAhora(fechaTurno.plusDays(1).atTime(1, 0));
        Empleado empleado = empleadoActivo();
        empleado.setActivo(false);
        Turno turno = turnoNocturno();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setFecha(fechaTurno);
        asistencia.setHoraEntrada(LocalTime.of(22, 0));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asistenciaRepository.findByPersonal_IdAndFecha(7, fechaTurno))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(Asistencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponseDTO respuesta = asistenciaService.registrarSalidaManual(7);

        assertEquals(fechaTurno, respuesta.getFecha());
        assertEquals("salida", respuesta.getTipoRegistro());
        assertEquals(LocalTime.of(1, 0), respuesta.getHoraSalida());
        verify(asistenciaRepository).save(asistencia);
    }

    @Test
    void tercerEscaneoSeRechazaSinModificarAsistencia() {
        Empleado empleado = empleadoActivo();
        Turno turno = turnoVigente();
        stubCredencialQr();
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setHoraEntrada(LocalTime.now(clock).minusHours(1));
        asistencia.setHoraSalida(LocalTime.now(clock).minusMinutes(1));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now(clock).minusDays(1)))
                .thenReturn(List.of());
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
        turno.setHoraEntrada(LocalTime.now(clock).plusMinutes(5));
        Asistencia asistencia = asistenciaExistente(empleado, turno);
        asistencia.setHoraEntrada(LocalTime.now(clock).minusMinutes(1));
        when(empleadoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(empleado));
        when(asignacionTurnoRepository.buscarVigentes(7, LocalDate.now(clock).minusDays(1)))
                .thenReturn(List.of());
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
                fijarAhora(LocalDate.now(clock).atTime(10, 0));
                Empleado empleado = empleadoActivo();
                Asistencia asistencia = asistenciaExistente(empleado, turnoVigente());
                asistencia.setHoraEntrada(LocalTime.now(clock).minusHours(2).minusMinutes(15));
                asistencia.setHoraSalida(null);
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                        LocalDate.now(), null, null))
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
        void listarHoyIncluyeEntradaNocturnaAbiertaDelDiaAnteriorConDuracionCorrecta() {
                LocalDate fechaTurno = LocalDate.now(clock);
                fijarAhora(fechaTurno.plusDays(1).atTime(1, 0));
                Empleado empleado = empleadoActivo();
                Asistencia asistencia = asistenciaExistente(empleado, turnoNocturno());
                asistencia.setFecha(fechaTurno);
                asistencia.setHoraEntrada(LocalTime.of(22, 0));
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                        fechaTurno.plusDays(1), null, null)).thenReturn(List.of());
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                        fechaTurno, null, null)).thenReturn(List.of(asistencia));

                List<AsistenciaHoyDTO> respuesta = asistenciaService.listarHoy();

                assertEquals(1, respuesta.size());
                assertEquals(11, respuesta.getFirst().getAsistenciaId());
                assertEquals("3:00", respuesta.getFirst().getHorasTrabajadas());
                assertEquals(180, respuesta.getFirst().getMinutosTrabajados());
                assertEquals(null, respuesta.getFirst().getHoraSalida());
        }

        @Test
        void listarHoyCalculaHorasConSalidaRegistrada() {
                Empleado empleado = empleadoActivo();
                Asistencia asistencia = asistenciaExistente(empleado, turnoVigente());
                asistencia.setHoraEntrada(LocalTime.of(8, 0));
                asistencia.setHoraSalida(LocalTime.of(16, 30));
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                        LocalDate.now(), null, null))
                                .thenReturn(List.of(asistencia));

                AsistenciaHoyDTO respuesta = asistenciaService.listarHoy().getFirst();

                assertEquals("8:30", respuesta.getHorasTrabajadas());
                assertEquals(510, respuesta.getMinutosTrabajados());
        }

        @Test
        void listarHoyReenviaFiltrosDeCargoYTurno() {
                when(asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                        LocalDate.now(clock), 8, 2)).thenReturn(List.of());

                assertEquals(List.of(), asistenciaService.listarHoy(8, 2));

                verify(asistenciaRepository).findAllByFechaWithPersonalOrderByHoraEntrada(
                        LocalDate.now(clock), 8, 2);
        }

            @Test
            void historicoAplicaFiltrosYNormalizaEstado() {
                when(asistenciaRepository.buscarHistorico(LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 29), 7, 3, null, 2, "tardanza"))
                        .thenReturn(List.of());

                List<AsistenciaResponseDTO> respuesta = asistenciaService.buscarHistorico(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 29),
                        7, 3, null, 2, " TARDANZA ");

                assertEquals(List.of(), respuesta);
                verify(asistenciaRepository).buscarHistorico(LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 29), 7, 3, null, 2, "tardanza");
            }

            @Test
            void ausenciasAceptaFiltroPorCargo() {
                LocalDate hoy = LocalDate.now();
                Empleado empleadoCoincidente = empleadoActivo();
                Cargo cargoCoincidente = new Cargo();
                cargoCoincidente.setId(8);
                empleadoCoincidente.setCargo(cargoCoincidente);
                Empleado empleadoOtroCargo = empleadoActivo();
                empleadoOtroCargo.setId(9);
                Cargo otroCargo = new Cargo();
                otroCargo.setId(9);
                empleadoOtroCargo.setCargo(otroCargo);

                AsignacionTurno asignacionCoincidente = asignacion(turnoVigente());
                asignacionCoincidente.setPersonal(empleadoCoincidente);
                AsignacionTurno asignacionOtroCargo = asignacion(turnoVigente());
                asignacionOtroCargo.setPersonal(empleadoOtroCargo);
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy, hoy))
                        .thenReturn(List.of(asignacionCoincidente, asignacionOtroCargo));
                when(asistenciaRepository.findAllByFechaBetween(hoy, hoy)).thenReturn(List.of());

                List<AusenciaDTO> respuesta = asistenciaService.listarAusencias(
                        hoy, hoy, null, null, 8, null);

                assertEquals(1, respuesta.size());
                assertEquals(7, respuesta.getFirst().getEmpleadoId());
            }

            @Test
            void rechazaHistoricoMayorAUnAnio() {
                ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                        () -> asistenciaService.buscarHistorico(LocalDate.now().minusDays(367),
                                LocalDate.now(), null, null, null, null));

                assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
            }

            @Test
            void listaAusenciasSoloEnDiasLaborablesSinAsistencia() {
                LocalDate hoy = LocalDate.now();
                Empleado empleado = empleadoActivo();
                Turno turno = turnoVigente();
                AsignacionTurno asignacion = asignacion(turno);
                asignacion.setPersonal(empleado);
                asignacion.setFechaDesde(hoy.minusDays(2));
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy.minusDays(2), hoy))
                        .thenReturn(List.of(asignacion));
                when(asistenciaRepository.findAllByFechaBetween(hoy.minusDays(2), hoy))
                        .thenReturn(List.of());

                List<AusenciaDTO> ausencias = asistenciaService.listarAusencias(
                        hoy.minusDays(2), hoy, null, null, null);

                assertEquals(3, ausencias.size());
                assertEquals("ausente", ausencias.getFirst().getEstado());
                assertEquals(7, ausencias.getFirst().getEmpleadoId());
            }

            @Test
            void exportaLasDosAusenciasCalculadasComoFilasDelReporte() throws Exception {
                LocalDate hoy = LocalDate.now(clock);
                Turno turno = turnoVigente();
                Empleado ana = empleadoActivo();
                Empleado luis = empleadoActivo();
                luis.setId(8);
                luis.setNombre("Luis");
                luis.setApellido("Gómez");
                AsignacionTurno asignacionAna = asignacion(turno);
                asignacionAna.setPersonal(ana);
                asignacionAna.setFechaDesde(hoy);
                AsignacionTurno asignacionLuis = asignacion(turno);
                asignacionLuis.setPersonal(luis);
                asignacionLuis.setFechaDesde(hoy);
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy, hoy))
                        .thenReturn(List.of(asignacionAna, asignacionLuis));
                when(asistenciaRepository.findAllByFechaBetween(hoy, hoy)).thenReturn(List.of());

                AsistenciaPdfGenerator pdfGenerator = new AsistenciaPdfGenerator();
                ReflectionTestUtils.setField(pdfGenerator, "institucion", "Institución de prueba");
                ReflectionTestUtils.setField(pdfGenerator, "municipio", "San Francisco");
                ReflectionTestUtils.setField(pdfGenerator, "estado", "Zulia");
                ReflectionTestUtils.setField(pdfGenerator, "codigoPlantel", "OD34942317");
                AsistenciaService servicioConPdf = new AsistenciaService(
                        asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                        credencialQrRepository, jwtProvider, clock, pdfGenerator);

                byte[] pdf = servicioConPdf.generarHistoricoPdf(
                        hoy, hoy, null, null, null, null, null);
                PdfReader reader = new PdfReader(pdf);
                String texto = new PdfTextExtractor(reader).getTextFromPage(1);
                reader.close();

                assertTrue(texto.contains("Ana Pérez"));
                assertTrue(texto.contains("Luis Gómez"));
                assertEquals(2, servicioConPdf.listarAusencias(hoy, hoy, null, null, null).size());
            }

            @Test
            void asistenciaExistenteEvitaFalsaAusencia() {
                LocalDate hoy = LocalDate.now();
                Empleado empleado = empleadoActivo();
                Turno turno = turnoVigente();
                AsignacionTurno asignacion = asignacion(turno);
                asignacion.setPersonal(empleado);
                asignacion.setFechaDesde(hoy);
                Asistencia asistencia = asistenciaExistente(empleado, turno);
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy, hoy))
                        .thenReturn(List.of(asignacion));
                when(asistenciaRepository.findAllByFechaBetween(hoy, hoy)).thenReturn(List.of(asistencia));

                List<AusenciaDTO> ausencias = asistenciaService.listarAusencias(hoy, hoy, null, null, null);

                assertEquals(List.of(), ausencias);
            }

            @Test
            void rechazaFechasFuturasEnElHistorico() {
                LocalDate manana = LocalDate.now().plusDays(1);

                ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                        () -> asistenciaService.listarAusencias(
                                manana, manana, null, null, null));

                assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
                verify(asignacionTurnoRepository, never())
                        .buscarAsignacionesActivasEnRango(any(LocalDate.class), any(LocalDate.class));
            }

        @Test
        void noGeneraAusenciaEnDiaLibreDelTurno() {
                LocalDate hoy = LocalDate.now();
                Empleado empleado = empleadoActivo();
                Turno turno = turnoVigente();
                turno.setLunes(false);
                turno.setMartes(false);
                turno.setMiercoles(false);
                turno.setJueves(false);
                turno.setViernes(false);
                turno.setSabado(false);
                turno.setDomingo(false);
                AsignacionTurno asignacion = asignacion(turno);
                asignacion.setPersonal(empleado);
                asignacion.setFechaDesde(hoy);
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy, hoy))
                                .thenReturn(List.of(asignacion));
                when(asistenciaRepository.findAllByFechaBetween(hoy, hoy)).thenReturn(List.of());

                List<AusenciaDTO> ausencias = asistenciaService.listarAusencias(hoy, hoy, null, null, null);

                assertEquals(List.of(), ausencias);
        }

        @Test
        void rechazaEstadoDeFiltroNoReconocido() {
                ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                                () -> asistenciaService.buscarHistorico(LocalDate.now(), LocalDate.now(),
                                                null, null, null, "llegó tarde"));

                assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        }

        @Test
        void incluyeAusenciaRegistradaExplicitamente() {
                LocalDate hoy = LocalDate.now();
                Asistencia asistencia = asistenciaExistente(empleadoActivo(), turnoVigente());
                asistencia.setEstado("ausente");
                when(asignacionTurnoRepository.buscarAsignacionesActivasEnRango(hoy, hoy))
                        .thenReturn(List.of());
                when(asistenciaRepository.findAllByFechaBetween(hoy, hoy)).thenReturn(List.of(asistencia));

                List<AusenciaDTO> ausencias = asistenciaService.listarAusencias(hoy, hoy, null, null, null);

                assertEquals(1, ausencias.size());
                assertEquals("ausente", ausencias.getFirst().getEstado());
                assertEquals("Ana", ausencias.getFirst().getNombre());
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
                7, QR_CREDENTIAL_ID, Instant.now(clock).plusSeconds(3600)));
        CredencialQr credencial = new CredencialQr();
        credencial.setId(QR_CREDENTIAL_ID);
        credencial.setActiva(true);
        credencial.setExpiraEn(LocalDateTime.now(clock).plusHours(1));
        when(credencialQrRepository.findByIdAndEmpleado_IdAndActivaTrueAndExpiraEnAfter(
                eq(QR_CREDENTIAL_ID), eq(7), any(LocalDateTime.class)))
                .thenReturn(Optional.of(credencial));
    }

    private void fijarAhora(LocalDateTime ahora) {
        clock = Clock.fixed(ahora.atZone(clock.getZone()).toInstant(), clock.getZone());
        asistenciaService = new AsistenciaService(
                asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider, clock);
    }

    private Turno turnoNocturno() {
        Turno turno = turnoVigente();
        turno.setNombre("Nocturno");
        turno.setHoraEntrada(LocalTime.of(22, 0));
        turno.setHoraSalida(LocalTime.of(6, 0));
        turno.setToleranciaMin((short) 0);
        turno.setMinutosSalidaAnticipadaPermitidos((short) 0);
        return turno;
    }

    private Turno turnoVigente() {
        Turno turno = new Turno();
        turno.setId(2);
        turno.setNombre("Diurno");
        turno.setHoraEntrada(LocalTime.now(clock).minusHours(1));
        turno.setHoraSalida(LocalTime.now(clock).plusHours(1));
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
        asistencia.setCreadoEn(LocalDateTime.now(clock));
        return asistencia;
    }
}