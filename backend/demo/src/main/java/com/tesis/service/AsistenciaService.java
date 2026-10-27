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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final AsistenciaPdfGenerator asistenciaPdfGenerator;

    @Autowired
    public AsistenciaService(AsistenciaRepository asistenciaRepository,
                             AsignacionTurnoRepository asignacionTurnoRepository,
                             EmpleadoRepository empleadoRepository,
                             CredencialQrRepository credencialQrRepository,
                             JwtProvider jwtProvider,
                             AsistenciaPdfGenerator asistenciaPdfGenerator) {
        this(asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider, Clock.systemDefaultZone(), asistenciaPdfGenerator);
    }

    AsistenciaService(AsistenciaRepository asistenciaRepository,
                      AsignacionTurnoRepository asignacionTurnoRepository,
                      EmpleadoRepository empleadoRepository,
                      CredencialQrRepository credencialQrRepository,
                      JwtProvider jwtProvider,
                      Clock clock) {
        this(asistenciaRepository, asignacionTurnoRepository, empleadoRepository,
                credencialQrRepository, jwtProvider, clock, new AsistenciaPdfGenerator());
    }

    AsistenciaService(AsistenciaRepository asistenciaRepository,
                      AsignacionTurnoRepository asignacionTurnoRepository,
                      EmpleadoRepository empleadoRepository,
                      CredencialQrRepository credencialQrRepository,
                      JwtProvider jwtProvider,
                      Clock clock,
                      AsistenciaPdfGenerator asistenciaPdfGenerator) {
        this.asistenciaRepository = asistenciaRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.credencialQrRepository = credencialQrRepository;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
        this.asistenciaPdfGenerator = asistenciaPdfGenerator;
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
        LocalDate fechaHoy = ahora.toLocalDate();
        LocalTime horaActual = ahora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        AsignacionTurno asignacionNocturnaAnterior = buscarTurnoNocturnoAnterior(
                empleadoId, fechaHoy, horaActual);
        LocalDate fecha = asignacionNocturnaAnterior == null ? fechaHoy : fechaHoy.minusDays(1);
        AsignacionTurno asignacion = Optional.ofNullable(asignacionNocturnaAnterior)
                .orElseGet(() -> asignacionTurnoRepository.buscarVigentes(empleadoId, fechaHoy)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "El empleado no tiene un turno asignado para hoy")));
        Turno turno = asignacion.getTurno();
        if (asignacionNocturnaAnterior == null && !turnoAplicaHoy(turno, fecha.getDayOfWeek())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno asignado no aplica para el día de hoy");
        }

        Asistencia asistencia = asistenciaRepository.findByPersonal_IdAndFecha(empleadoId, fecha)
                .orElseGet(() -> nuevaAsistencia(empleado, turno, fecha));
        String tipoRegistro;

        if (asistencia.getHoraEntrada() == null) {
            if (asignacionNocturnaAnterior == null
                    && esTurnoNocturno(turno) && horaActual.isBefore(turno.getHoraEntrada())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar entrada fuera del horario del turno");
            }
            if (!esTurnoNocturno(turno) && horaActual.isAfter(turno.getHoraSalida())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar entrada después de finalizar el turno");
            }
            registrarEntrada(asistencia, turno, ahora);
            tipoRegistro = "entrada";
        } else if (asistencia.getHoraSalida() == null) {
            registrarSalidaValidada(asistencia, ahora);
            tipoRegistro = "salida";
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La asistencia ya tiene entrada y salida registradas");
        }

        return toResponse(asistenciaRepository.save(asistencia), tipoRegistro);
    }

    public AsistenciaResponseDTO registrarSalidaManual(Integer empleadoId) {
        empleadoRepository.findByIdForUpdate(empleadoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate fechaHoy = ahora.toLocalDate();
        Asistencia asistenciaNocturnaAbierta = asistenciaRepository.findByPersonal_IdAndFecha(
                        empleadoId, fechaHoy.minusDays(1))
                .filter(registro -> registro.getHoraEntrada() != null
                        && registro.getHoraSalida() == null
                        && esTurnoNocturno(registro.getTurno())
                        && turnoAplicaHoy(registro.getTurno(), registro.getFecha().getDayOfWeek()))
                .orElse(null);
        Asistencia asistencia = Optional.ofNullable(asistenciaNocturnaAbierta)
                .orElseGet(() -> asistenciaRepository.findByPersonal_IdAndFecha(empleadoId, fechaHoy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay una entrada registrada para hoy")));
        if (asistencia.getHoraEntrada() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede registrar salida sin una entrada previa");
        }
        if (asistencia.getHoraSalida() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La asistencia de hoy ya tiene salida registrada");
        }
        registrarSalidaValidada(asistencia, ahora);
        return toResponse(asistenciaRepository.save(asistencia), "salida");
    }

    @Transactional(readOnly = true)
    public List<AsistenciaHoyDTO> listarHoy() {
        return listarHoy(null, null);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaHoyDTO> listarHoy(Integer cargoId, Integer turnoId) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        List<Asistencia> registrosHoy = asistenciaRepository.findAllByFechaWithPersonalOrderByHoraEntrada(
                hoy, cargoId, turnoId);
        List<Asistencia> entradasNocturnasAbiertas = asistenciaRepository
                .findAllByFechaWithPersonalOrderByHoraEntrada(hoy.minusDays(1), cargoId, turnoId)
                .stream()
                .filter(asistencia -> asistencia.getHoraEntrada() != null
                        && asistencia.getHoraSalida() == null
                        && esTurnoNocturno(asistencia.getTurno()))
                .toList();
        return java.util.stream.Stream.concat(registrosHoy.stream(), entradasNocturnasAbiertas.stream())
                .sorted(Comparator.comparing(Asistencia::getFecha))
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
        return buscarHistorico(desde, hasta, empleadoId, departamentoId, null, turnoId, estado);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponseDTO> buscarHistorico(LocalDate desde,
                                                       LocalDate hasta,
                                                       Integer empleadoId,
                                                       Integer departamentoId,
                                                       Integer cargoId,
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
                        departamentoId, cargoId, turnoId,
                        estadoNormalizado == null ? null : estadoNormalizado.toLowerCase()).stream()
                .map(asistencia -> toResponse(asistencia, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] generarHistoricoPdf(LocalDate desde,
                                      LocalDate hasta,
                                      Integer empleadoId,
                                      Integer departamentoId,
                                      Integer cargoId,
                                      Integer turnoId,
                                      String estado) {
        List<AsistenciaResponseDTO> registros = buscarHistorico(
                desde, hasta, empleadoId, departamentoId, cargoId, turnoId, estado);
        List<AusenciaDTO> ausencias = listarAusencias(
                desde, hasta, empleadoId, departamentoId, cargoId, turnoId);
        Set<String> asistenciasRegistradas = registros.stream()
                .map(registro -> claveAsistencia(registro.getEmpleadoId(), registro.getFecha()))
                .collect(Collectors.toSet());
        List<AusenciaDTO> ausenciasSinRegistro = ausencias.stream()
                .filter(ausencia -> !asistenciasRegistradas.contains(
                        claveAsistencia(ausencia.getEmpleadoId(), ausencia.getFecha())))
                .toList();
        return asistenciaPdfGenerator.generar(
                desde, hasta, registros, ausenciasSinRegistro, estado == null || estado.isBlank()
                        || "ausente".equalsIgnoreCase(estado.trim()));
    }

    @Transactional(readOnly = true)
    public List<AusenciaDTO> listarAusencias(LocalDate desde,
                                              LocalDate hasta,
                                              Integer empleadoId,
                                              Integer departamentoId,
                                              Integer turnoId) {
        return listarAusencias(desde, hasta, empleadoId, departamentoId, null, turnoId);
    }

    @Transactional(readOnly = true)
    public List<AusenciaDTO> listarAusencias(LocalDate desde,
                                              LocalDate hasta,
                                              Integer empleadoId,
                                              Integer departamentoId,
                                              Integer cargoId,
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
                .filter(asignacion -> cargoId == null
                        || (asignacion.getPersonal().getCargo() != null
                        && cargoId.equals(asignacion.getPersonal().getCargo().getId())))
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
                    && (cargoId == null || (empleado.getCargo() != null
                    && cargoId.equals(empleado.getCargo().getId())))
                    && (turnoId == null || (turno != null && turnoId.equals(turno.getId())))) {
                ausencias.add(new AusenciaDTO(empleado.getId(), empleado.getNombre(), empleado.getApellido(),
                        empleado.getCedula(),
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
                                empleado.getCedula(),
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
        if (hasta.isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha hasta no puede ser posterior a hoy");
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
            Turno turno = asistencia.getTurno();
            if (esTurnoNocturno(turno) && !horaEntrada.isAfter(turno.getHoraSalida())) {
                fechaEntrada = fechaEntrada.plusDays(1);
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

    private void registrarEntrada(Asistencia asistencia, Turno turno, LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        asistencia.setTurno(turno);
        asistencia.setHoraEntrada(hora);
        LocalDate fechaEntrada = asistencia.getFecha();
        if (esTurnoNocturno(turno) && !hora.isAfter(turno.getHoraSalida())) {
            fechaEntrada = fechaEntrada.plusDays(1);
        }
        LocalDateTime inicioTurno = LocalDateTime.of(asistencia.getFecha(), turno.getHoraEntrada());
        LocalDateTime entradaReal = LocalDateTime.of(fechaEntrada, hora);
        long minutosTardanza = Math.max(0, Duration.between(inicioTurno, entradaReal).toMinutes());
        asistencia.setMinutosTardanza((short) Math.min(Short.MAX_VALUE, minutosTardanza));
        long tolerancia = turno.getToleranciaMin() == null ? 0 : turno.getToleranciaMin();
        asistencia.setEstado(minutosTardanza > tolerancia ? "tardanza" : "presente");
    }

    private void registrarSalida(Asistencia asistencia, Turno turno, LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        asistencia.setHoraSalida(hora);
        LocalDateTime finTurno = LocalDateTime.of(asistencia.getFecha(), turno.getHoraSalida());
        if (esTurnoNocturno(turno)) {
            finTurno = finTurno.plusDays(1);
        }
        if (fechaHora.isBefore(finTurno)) {
            long minutosAnticipados = Duration.between(fechaHora, finTurno).toMinutes();
            long permitidos = turno.getMinutosSalidaAnticipadaPermitidos() == null
                    ? 0 : turno.getMinutosSalidaAnticipadaPermitidos();
            if (minutosAnticipados > permitidos) {
                asistencia.setEstado("salida_anticipada");
            }
        } else if (fechaHora.isAfter(finTurno)) {
            long minutosExtra = Duration.between(finTurno, fechaHora).toMinutes();
            asistencia.setMinutosExtra((short) Math.min(Short.MAX_VALUE, minutosExtra));
        }
    }

    private void registrarSalidaValidada(Asistencia asistencia, LocalDateTime fechaHora) {
        Turno turno = asistencia.getTurno();
        LocalDateTime fechaHoraEntrada = fechaHoraEntrada(asistencia);
        if (turno != null) {
            LocalDateTime inicioTurno = LocalDateTime.of(asistencia.getFecha(), turno.getHoraEntrada());
            if (fechaHora.isBefore(inicioTurno)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede registrar salida antes del inicio del turno");
            }
        }
        if (!fechaHora.isAfter(fechaHoraEntrada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La salida debe registrarse después de la entrada");
        }
        if (turno == null) {
            asistencia.setHoraSalida(fechaHora.toLocalTime().truncatedTo(ChronoUnit.SECONDS));
        } else {
            registrarSalida(asistencia, turno, fechaHora);
        }
    }

    private LocalDateTime fechaHoraEntrada(Asistencia asistencia) {
        LocalDate fechaEntrada = asistencia.getFecha();
        Turno turno = asistencia.getTurno();
        if (esTurnoNocturno(turno) && !asistencia.getHoraEntrada().isAfter(turno.getHoraSalida())) {
            fechaEntrada = fechaEntrada.plusDays(1);
        }
        return LocalDateTime.of(fechaEntrada, asistencia.getHoraEntrada());
    }

    private AsignacionTurno buscarTurnoNocturnoAnterior(Integer empleadoId,
                                                        LocalDate hoy,
                                                        LocalTime horaActual) {
        LocalDate ayer = hoy.minusDays(1);
        return asignacionTurnoRepository.buscarVigentes(empleadoId, ayer).stream()
                .filter(asignacion -> esTurnoNocturno(asignacion.getTurno()))
                .filter(asignacion -> turnoAplicaHoy(asignacion.getTurno(), ayer.getDayOfWeek()))
                .filter(asignacion -> horaActual.isBefore(asignacion.getTurno().getHoraSalida())
                        || asistenciaRepository.findByPersonal_IdAndFecha(empleadoId, ayer)
                        .filter(asistencia -> asistencia.getHoraEntrada() != null
                                && asistencia.getHoraSalida() == null)
                        .isPresent())
                .findFirst()
                .orElse(null);
    }

    private boolean esTurnoNocturno(Turno turno) {
        return turno != null && turno.getHoraSalida().isBefore(turno.getHoraEntrada());
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
                empleado.getCedula(),
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