package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CredencialQrRepository credencialQrRepository;
    private final JwtProvider jwtProvider;

    public AsistenciaService(AsistenciaRepository asistenciaRepository,
                             AsignacionTurnoRepository asignacionTurnoRepository,
                             EmpleadoRepository empleadoRepository,
                             CredencialQrRepository credencialQrRepository,
                             JwtProvider jwtProvider) {
        this.asistenciaRepository = asistenciaRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.credencialQrRepository = credencialQrRepository;
        this.jwtProvider = jwtProvider;
    }

    public AsistenciaResponseDTO registrarEscaneo(String qrToken) {
        JwtProvider.QrClaims qrClaims;
        try {
            qrClaims = jwtProvider.validarTokenQr(qrToken);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "QR inválido o vencido", exception);
        }

        Integer empleadoId = qrClaims.empleadoId();
        Empleado empleado = empleadoRepository.findByIdForUpdate(empleadoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        CredencialQr credencial = credencialQrRepository
                .findByIdAndEmpleado_IdAndActivaTrueAndExpiraEnAfter(
                        qrClaims.credencialId(), empleadoId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "QR revocado o vencido"));
        if (credencial.getExpiraEn().isBefore(LocalDateTime.ofInstant(
                qrClaims.expiracion(), java.time.ZoneId.systemDefault()).minusSeconds(1))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "QR inválido o vencido");
        }
        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El empleado está inactivo");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDate fecha = ahora.toLocalDate();
        AsignacionTurno asignacion = asignacionTurnoRepository.buscarVigentes(empleadoId, fecha)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "El empleado no tiene un turno asignado para hoy"));
        Turno turno = asignacion.getTurno();
        if (!turnoAplicaHoy(turno, fecha.getDayOfWeek())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno asignado no aplica para el día de hoy");
        }

        Asistencia asistencia = asistenciaRepository.findByPersonal_IdAndFecha(empleadoId, fecha)
                .orElseGet(() -> nuevaAsistencia(empleado, turno, fecha));
        LocalTime horaActual = ahora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        String tipoRegistro;

        if (asistencia.getHoraEntrada() == null) {
            if (horaActual.isAfter(turno.getHoraSalida())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar entrada después de finalizar el turno");
            }
            registrarEntrada(asistencia, turno, horaActual);
            tipoRegistro = "entrada";
        } else if (asistencia.getHoraSalida() == null) {
            if (horaActual.isBefore(turno.getHoraEntrada())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar salida antes del inicio del turno");
            }
            if (!horaActual.isAfter(asistencia.getHoraEntrada())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La salida debe registrarse después de la entrada");
            }
            registrarSalida(asistencia, turno, horaActual);
            tipoRegistro = "salida";
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La asistencia de hoy ya tiene entrada y salida registradas");
        }

        return toResponse(asistenciaRepository.save(asistencia), tipoRegistro);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> listarPorEmpleado(Integer empleadoId,
                                                         LocalDate desde,
                                                         LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (!empleadoRepository.existsById(empleadoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado");
        }
        return asistenciaRepository.findByPersonal_IdAndFechaBetweenOrderByFechaDesc(
                        empleadoId, desde, hasta).stream()
                .map(asistencia -> toResponse(asistencia, null))
                .toList();
    }

    private Asistencia nuevaAsistencia(Empleado empleado, Turno turno, LocalDate fecha) {
        Asistencia asistencia = new Asistencia();
        asistencia.setPersonal(empleado);
        asistencia.setTurno(turno);
        asistencia.setFecha(fecha);
        asistencia.setEstado("presente");
        asistencia.setMinutosTardanza((short) 0);
        asistencia.setMinutosExtra((short) 0);
        asistencia.setCorregido(false);
        return asistencia;
    }

    private void registrarEntrada(Asistencia asistencia, Turno turno, LocalTime hora) {
        asistencia.setTurno(turno);
        asistencia.setHoraEntrada(hora);
        long minutosTardanza = hora.isAfter(turno.getHoraEntrada())
                ? Duration.between(turno.getHoraEntrada(), hora).toMinutes() : 0;
        asistencia.setMinutosTardanza((short) Math.min(Short.MAX_VALUE, minutosTardanza));
        long tolerancia = turno.getToleranciaMin() == null ? 0 : turno.getToleranciaMin();
        asistencia.setEstado(minutosTardanza > tolerancia ? "tardanza" : "presente");
    }

    private void registrarSalida(Asistencia asistencia, Turno turno, LocalTime hora) {
        asistencia.setHoraSalida(hora);
        if (hora.isBefore(turno.getHoraSalida())) {
            long minutosAnticipados = Duration.between(hora, turno.getHoraSalida()).toMinutes();
            long permitidos = turno.getMinutosSalidaAnticipadaPermitidos() == null
                    ? 0 : turno.getMinutosSalidaAnticipadaPermitidos();
            if (minutosAnticipados > permitidos) {
                asistencia.setEstado("salida_anticipada");
            }
        } else if (hora.isAfter(turno.getHoraSalida())) {
            long minutosExtra = ChronoUnit.MINUTES.between(turno.getHoraSalida(), hora);
            asistencia.setMinutosExtra((short) Math.min(Short.MAX_VALUE, minutosExtra));
        }
    }

    private boolean turnoAplicaHoy(Turno turno, DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> Boolean.TRUE.equals(turno.getLunes());
            case TUESDAY -> Boolean.TRUE.equals(turno.getMartes());
            case WEDNESDAY -> Boolean.TRUE.equals(turno.getMiercoles());
            case THURSDAY -> Boolean.TRUE.equals(turno.getJueves());
            case FRIDAY -> Boolean.TRUE.equals(turno.getViernes());
            case SATURDAY -> Boolean.TRUE.equals(turno.getSabado());
            case SUNDAY -> Boolean.TRUE.equals(turno.getDomingo());
        };
    }

    private AsistenciaResponseDTO toResponse(Asistencia asistencia, String tipoRegistro) {
        Empleado empleado = asistencia.getPersonal();
        Turno turno = asistencia.getTurno();
        return new AsistenciaResponseDTO(
                asistencia.getId(),
                empleado.getId(),
                empleado.getNombre() + " " + empleado.getApellido(),
                asistencia.getFecha(),
                turno == null ? null : turno.getId(),
                turno == null ? null : turno.getNombre(),
                asistencia.getHoraEntrada(),
                asistencia.getHoraSalida(),
                asistencia.getEstado(),
                asistencia.getMinutosTardanza(),
                asistencia.getMinutosExtra(),
                tipoRegistro,
                asistencia.getCreadoEn());
    }
}