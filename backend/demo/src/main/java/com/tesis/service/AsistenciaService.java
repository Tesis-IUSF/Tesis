package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AsistenciaHoyDTO;
import com.tesis.dto.AsistenciaDTO.AusenciaDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AsistenciaService {

    private static final long MAX_REPORT_DAYS = 366;
    private static final Set<String> ESTADOS_ASISTENCIA = Set.of(
            "presente", "tardanza", "salida_anticipada", "ausente", "permiso", "feriado", "libre");

    private final AsistenciaRepository asistenciaRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CredencialQrRepository credencialQrRepository;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    @Autowired
    public AsistenciaService(AsistenciaRepository asistenciaRepository,
                             AsignacionTurnoRepository asignacionTurnoRepository,
                             EmpleadoRepository empleadoRepository,
                             CredencialQrRepository credencialQrRepository,
                             JwtProvider jwtProvider) {
        this(asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider, Clock.systemDefaultZone());
    }

    AsistenciaService(AsistenciaRepository asistenciaRepository,
                      AsignacionTurnoRepository asignacionTurnoRepository,
                      EmpleadoRepository empleadoRepository,
                      CredencialQrRepository credencialQrRepository,
                      JwtProvider jwtProvider,
                      Clock clock) {
        this.asistenciaRepository = asistenciaRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.credencialQrRepository = credencialQrRepository;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
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
                        qrClaims.credencialId(), empleadoId, LocalDateTime.now(clock))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "QR revocado o vencido"));
        if (credencial.getExpiraEn().isBefore(LocalDateTime.ofInstant(
                qrClaims.expiracion(), java.time.ZoneId.systemDefault()).minusSeconds(1))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "QR inválido o vencido");
        }
        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El empleado está inactivo");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
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
    public List<AsistenciaHoyDTO> listarHoy() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(ahora.toLocalDate())
                .stream()
                .map(asistencia -> toAsistenciaHoy(asistencia, ahora))
                .toList();
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

    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> buscarHistorico(LocalDate desde,
                                                       LocalDate hasta,
                                                       Integer empleadoId,
                                                       Integer departamentoId,
                                                       Integer turnoId,
                                                       String estado) {
        validarRangoFechas(desde, hasta);
        String estadoNormalizado = estado == null || estado.isBlank() ? null : estado.trim();
        if (estadoNormalizado != null
            && !ESTADOS_ASISTENCIA.contains(estadoNormalizado.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El estado de asistencia no es válido");
        }
        return asistenciaRepository.buscarHistorico(desde, hasta, empleadoId,
                        departamentoId, turnoId,
                        estadoNormalizado == null ? null : estadoNormalizado.toLowerCase()).stream()
                .map(asistencia -> toResponse(asistencia, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AusenciaDTO> listarAusencias(LocalDate desde,
                                              LocalDate hasta,
                                              Integer empleadoId,
                                              Integer departamentoId,
                                              Integer turnoId) {
        validarRangoFechas(desde, hasta);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaFinal = hasta.isAfter(hoy) ? hoy : hasta;
        if (desde.isAfter(fechaFinal)) {
            return List.of();
        }

        List<AsignacionTurno> asignaciones = asignacionTurnoRepository
                .buscarAsignacionesActivasEnRango(desde, fechaFinal).stream()
                .filter(asignacion -> empleadoId == null
                        || empleadoId.equals(asignacion.getPersonal().getId()))
                .filter(asignacion -> departamentoId == null
                        || (asignacion.getPersonal().getDepartamento() != null
                        && departamentoId.equals(asignacion.getPersonal().getDepartamento().getId())))
                .filter(asignacion -> turnoId == null || turnoId.equals(asignacion.getTurno().getId()))
                .toList();
        List<Asistencia> asistencias = asistenciaRepository.findAllByFechaBetween(desde, fechaFinal);
        Set<String> asistenciaRegistrada = new HashSet<>();
        for (Asistencia asistencia : asistencias) {
            asistenciaRegistrada.add(claveAsistencia(asistencia.getPersonal().getId(), asistencia.getFecha()));
        }

        List<AusenciaDTO> ausencias = new ArrayList<>();
        for (Asistencia asistencia : asistencias) {
            Empleado empleado = asistencia.getPersonal();
            Turno turno = asistencia.getTurno();
            if ("ausente".equalsIgnoreCase(asistencia.getEstado())
                    && (empleadoId == null || empleadoId.equals(empleado.getId()))
                    && (departamentoId == null || (empleado.getDepartamento() != null
                    && departamentoId.equals(empleado.getDepartamento().getId())))
                    && (turnoId == null || (turno != null && turnoId.equals(turno.getId())))) {
                ausencias.add(new AusenciaDTO(empleado.getId(), empleado.getNombre(), empleado.getApellido(),
                        asistencia.getFecha(), turno == null ? null : turno.getId(),
                        turno == null ? null : turno.getNombre(), "ausente"));
            }
        }
        for (LocalDate fecha = desde; !fecha.isAfter(fechaFinal); fecha = fecha.plusDays(1)) {
            Set<Integer> empleadosProcesados = new HashSet<>();
            for (AsignacionTurno asignacion : asignaciones) {
                if (!asignacion.getFechaDesde().isAfter(fecha)
                        && (asignacion.getFechaHasta() == null || !asignacion.getFechaHasta().isBefore(fecha))) {
                    Integer id = asignacion.getPersonal().getId();
                    if (empleadosProcesados.add(id)
                            && turnoAplicaHoy(asignacion.getTurno(), fecha.getDayOfWeek())
                            && !asistenciaRegistrada.contains(claveAsistencia(id, fecha))) {
                        Empleado empleado = asignacion.getPersonal();
                        Turno turno = asignacion.getTurno();
                        ausencias.add(new AusenciaDTO(id, empleado.getNombre(), empleado.getApellido(),
                                fecha, turno.getId(), turno.getNombre(), "ausente"));
                    }
                }
            }
        }
        return ausencias.stream()
                .sorted((primera, segunda) -> {
                    int porFecha = segunda.getFecha().compareTo(primera.getFecha());
                    if (porFecha != 0) {
                        return porFecha;
                    }
                    int porApellido = primera.getApellido().compareToIgnoreCase(segunda.getApellido());
                    return porApellido != 0 ? porApellido : primera.getNombre().compareToIgnoreCase(segunda.getNombre());
                })
                .toList();
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rango de fechas es inválido: desde debe ser anterior o igual a hasta");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_REPORT_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rango máximo permitido para el reporte es de 366 días");
        }
    }

    private String claveAsistencia(Integer empleadoId, LocalDate fecha) {
        return empleadoId + ":" + fecha;
    }

    private AsistenciaHoyDTO toAsistenciaHoy(Asistencia asistencia, LocalDateTime ahora) {
        LocalTime horaEntrada = asistencia.getHoraEntrada();
        Integer minutosTrabajados = null;
        String horasTrabajadas = null;
        if (horaEntrada != null) {
            LocalDate fechaEntrada = asistencia.getFecha();
            if (asistencia.getHoraSalida() == null && horaEntrada.isAfter(ahora.toLocalTime())) {
                fechaEntrada = fechaEntrada.minusDays(1);
            }
            LocalDateTime inicio = LocalDateTime.of(fechaEntrada, horaEntrada);
            LocalDateTime fin = asistencia.getHoraSalida() == null
                    ? ahora
                    : LocalDateTime.of(asistencia.getFecha(), asistencia.getHoraSalida());
            if (asistencia.getHoraSalida() != null && fin.isBefore(inicio)) {
                fin = fin.plusDays(1);
            }
            long minutos = Math.max(0, Duration.between(inicio, fin).toMinutes());
            minutosTrabajados = (int) Math.min(Integer.MAX_VALUE, minutos);
            horasTrabajadas = String.format("%d:%02d", minutos / 60, minutos % 60);
        }

        return new AsistenciaHoyDTO(
                asistencia.getId(),
                asistencia.getPersonal().getId(),
                asistencia.getPersonal().getNombre(),
                asistencia.getPersonal().getApellido(),
                horaEntrada,
                asistencia.getHoraSalida(),
                minutosTrabajados,
                horasTrabajadas,
                asistencia.getEstado());
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