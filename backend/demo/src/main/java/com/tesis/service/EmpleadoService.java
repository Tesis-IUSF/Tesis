package com.tesis.service;

import com.tesis.dto.EmpleadoDTO.EmpleadoRequestDTO;
import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import com.tesis.entity.Usuario;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.TurnoRepository;
import com.tesis.repository.UsuarioRepository;
import com.tesis.validation.CedulaVenezolana;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final CargoRepository cargoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;
    private final TurnoRepository turnoRepository;
    private final CredencialQrRepository credencialQrRepository;
    private final NominaPersonalPdfGenerator nominaPersonalPdfGenerator;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           CargoRepository cargoRepository,
                           DepartamentoRepository departamentoRepository,
                           UsuarioRepository usuarioRepository,
                           AsignacionTurnoRepository asignacionTurnoRepository,
                           TurnoRepository turnoRepository,
                           CredencialQrRepository credencialQrRepository,
                           NominaPersonalPdfGenerator nominaPersonalPdfGenerator) {
        this.empleadoRepository = empleadoRepository;
        this.cargoRepository = cargoRepository;
        this.departamentoRepository = departamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.turnoRepository = turnoRepository;
        this.credencialQrRepository = credencialQrRepository;
        this.nominaPersonalPdfGenerator = nominaPersonalPdfGenerator;
    }

    @Transactional(readOnly = true)
    public Page<EmpleadoResponseDTO> listar(Integer cargoId, Integer turnoId, Pageable pageable) {
        return listar(cargoId, turnoId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EmpleadoResponseDTO> listar(Pageable pageable) {
        return listar(null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EmpleadoResponseDTO> listar(Integer cargoId, Integer turnoId, Boolean activo, Pageable pageable) {
        Page<Empleado> empleados = empleadoRepository.buscarConFiltros(
                cargoId, turnoId, activo, LocalDate.now(), pageable);
        Map<Integer, AsignacionTurno> turnosVigentes = buscarTurnosVigentes(
            empleados.getContent().stream().map(empleado -> empleado.getId()).toList());
        return empleados.map(empleado -> toResponse(empleado, turnosVigentes.get(empleado.getId())));
    }

    @Transactional(readOnly = true)
    public byte[] generarNominaPDF(Integer cargoId, Integer turnoId, Boolean activo) {
        List<Empleado> empleados = empleadoRepository.buscarTodosConFiltros(
                cargoId, turnoId, activo, LocalDate.now());
        List<Integer> empleadoIds = empleados.stream().map(Empleado::getId).toList();
        Map<Integer, List<AsignacionTurno>> historialPorEmpleado = empleadoIds.isEmpty()
                ? Map.of()
                : asignacionTurnoRepository.buscarHistorialPorEmpleados(empleadoIds).stream()
                        .collect(Collectors.groupingBy(
                                asignacion -> asignacion.getPersonal().getId()));
        LocalDate hoy = LocalDate.now();
        List<EmpleadoResponseDTO> filas = empleados.stream()
                .map(empleado -> {
                    LocalDate fechaReferencia = Boolean.FALSE.equals(empleado.getActivo())
                            && empleado.getFechaEgreso() != null ? empleado.getFechaEgreso() : hoy;
                    AsignacionTurno asignacion = historialPorEmpleado
                            .getOrDefault(empleado.getId(), List.of()).stream()
                            .filter(item -> !item.getFechaDesde().isAfter(fechaReferencia))
                            .filter(item -> item.getFechaHasta() == null
                                    || !item.getFechaHasta().isBefore(fechaReferencia))
                            .findFirst()
                            .orElse(null);
                    return toResponse(empleado, asignacion);
                })
                .toList();
        return nominaPersonalPdfGenerator.generar(filas, activo == null || Boolean.FALSE.equals(activo));
    }

    @Transactional(readOnly = true)
    public EmpleadoResponseDTO obtener(Integer id) {
        return toResponse(buscarEmpleado(id));
    }

    public EmpleadoResponseDTO crear(EmpleadoRequestDTO request) {
        String cedula = normalizarYValidarCedula(request.getCedula());
        if (empleadoRepository.existsByCedula(cedula)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        if (request.getCargoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo es obligatorio");
        }
        if (request.getTurnoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El turno es obligatorio al crear un empleado");
        }
        if (request.getFechaIngreso() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de ingreso es obligatoria para asignar el turno");
        }
        Turno turno = buscarTurnoActivo(request.getTurnoId());
        Empleado empleado = new Empleado();
        aplicarRequest(request, empleado, true);
        empleado.setCedula(cedula);
        validarEstadoLaboral(Boolean.TRUE.equals(empleado.getActivo()), empleado.getFechaEgreso(),
                empleado.getFechaIngreso());
        Empleado empleadoGuardado = empleadoRepository.save(empleado);

        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setPersonal(empleadoGuardado);
        asignacion.setTurno(turno);
        asignacion.setFechaDesde(empleadoGuardado.getFechaIngreso());
        asignacion.setFechaHasta(empleadoGuardado.getFechaEgreso());
        AsignacionTurno asignacionGuardada = asignacionTurnoRepository.save(asignacion);
        return toResponse(empleadoGuardado, asignacionGuardada);
    }

    public EmpleadoResponseDTO actualizar(Integer id, EmpleadoRequestDTO request) {
        Empleado empleado = buscarEmpleado(id);
        String cedula = normalizarYValidarCedula(request.getCedula());
        if (empleadoRepository.existsByCedulaAndIdNot(cedula, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        if (request.getCargoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo es obligatorio");
        }
        aplicarRequest(request, empleado, false);
        empleado.setCedula(cedula);
        if (Boolean.FALSE.equals(empleado.getActivo()) && empleado.getFechaEgreso() == null) {
            empleado.setFechaEgreso(LocalDate.now());
        }
        validarEstadoLaboral(Boolean.TRUE.equals(empleado.getActivo()), empleado.getFechaEgreso(),
                empleado.getFechaIngreso());
        if (Boolean.FALSE.equals(empleado.getActivo())) {
            darDeBaja(empleado);
        }
        return toResponse(empleadoRepository.save(empleado));
    }

    public void eliminar(Integer id) {
        Empleado empleado = buscarEmpleado(id);
        darDeBaja(empleado);
        empleadoRepository.save(empleado);
    }

    private void darDeBaja(Empleado empleado) {
        if (empleado.getFechaEgreso() == null) {
            empleado.setFechaEgreso(LocalDate.now());
        }
        validarEstadoLaboral(false, empleado.getFechaEgreso(), empleado.getFechaIngreso());
        empleado.setActivo(false);
        LocalDate fechaEgreso = empleado.getFechaEgreso();
        asignacionTurnoRepository.buscarVigentesEnFecha(empleado.getId(), fechaEgreso).forEach(asignacion -> {
            if (asignacion.getFechaHasta() == null || asignacion.getFechaHasta().isAfter(fechaEgreso)) {
                asignacion.setFechaHasta(fechaEgreso);
                asignacionTurnoRepository.save(asignacion);
            }
        });
        credencialQrRepository.findByEmpleado_IdAndActivaTrue(empleado.getId()).forEach(credencial -> {
            credencial.setActiva(false);
            credencial.setRevocadaEn(LocalDateTime.now());
        });
    }

    private Empleado buscarEmpleado(Integer id) {
        return empleadoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
    }

    private Map<Integer, AsignacionTurno> buscarTurnosVigentes(List<Integer> empleadoIds) {
        if (empleadoIds.isEmpty()) {
            return Map.of();
        }
        return asignacionTurnoRepository.buscarVigentesPorEmpleados(empleadoIds, LocalDate.now()).stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getPersonal().getId(), Function.identity(),
                        (primera, ignorada) -> primera));
    }

    private Turno buscarTurnoActivo(Integer id) {
        Turno turno = turnoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (!Boolean.TRUE.equals(turno.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un turno inactivo");
        }
        return turno;
    }

    private String normalizarYValidarCedula(String cedula) {
        String normalizada = CedulaVenezolana.normalizar(cedula);
        if (!CedulaVenezolana.esValida(normalizada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CedulaVenezolana.MENSAJE);
        }
        return normalizada;
    }

    private void validarEstadoLaboral(boolean activo, LocalDate fechaEgreso, LocalDate fechaIngreso) {
        if (activo && fechaEgreso != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un empleado activo no puede tener fecha de egreso");
        }
        if (!activo && fechaEgreso == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de egreso es obligatoria para un empleado inactivo");
        }
        if (fechaEgreso != null && fechaIngreso != null && fechaEgreso.isBefore(fechaIngreso)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de egreso no puede ser anterior a la fecha de ingreso");
        }
    }

    private void aplicarRequest(EmpleadoRequestDTO request, Empleado empleado, boolean nuevo) {
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setCorreo(request.getCorreo());
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        empleado.setSexo(request.getSexo());
        if (request.getFotoUrl() != null) {
            empleado.setFotoUrl(request.getFotoUrl());
        }
        empleado.setCargo(buscarCargo(request.getCargoId()));
        empleado.setDepartamento(buscarDepartamento(request.getDepartamentoId()));
        empleado.setFechaIngreso(request.getFechaIngreso());
        if (request.getFechaEgreso() != null
                || nuevo
                || Boolean.TRUE.equals(request.getActivo())) {
            empleado.setFechaEgreso(request.getFechaEgreso());
        }
        empleado.setUsuario(buscarUsuario(request.getUsuarioId(), empleado.getId()));
        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        } else if (nuevo) {
            empleado.setActivo(true);
        }
    }

    private Cargo buscarCargo(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cargo es obligatorio");
        }
        return cargoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo no encontrado"));
    }

    private Departamento buscarDepartamento(Integer id) {
        if (id == null) {
            return null;
        }
        return departamentoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departamento no encontrado"));
    }

    private Usuario buscarUsuario(Integer id, Integer empleadoId) {
        if (id == null) {
            return null;
        }
        boolean usuarioYaAsignado = empleadoId == null
            ? empleadoRepository.existsByUsuario_Id(id)
            : empleadoRepository.existsByUsuario_IdAndIdNot(id, empleadoId);
        if (usuarioYaAsignado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario ya está vinculado a otro empleado");
        }
        return usuarioRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));
    }

    private EmpleadoResponseDTO toResponse(Empleado empleado) {
        Map<Integer, AsignacionTurno> turnos = buscarTurnosVigentes(List.of(empleado.getId()));
        return toResponse(empleado, turnos.get(empleado.getId()));
    }

    private EmpleadoResponseDTO toResponse(Empleado empleado, AsignacionTurno asignacionTurno) {
        return new EmpleadoResponseDTO(
                empleado.getId(),
                empleado.getNombre(),
                empleado.getApellido(),
                empleado.getCedula(),
                empleado.getCorreo(),
                empleado.getTelefono(),
                empleado.getFechaNacimiento(),
                empleado.getSexo(),
                empleado.getFotoUrl(),
                empleado.getCargo() == null ? null : empleado.getCargo().getId(),
                empleado.getCargo() == null ? null : empleado.getCargo().getNombreCargo(),
                empleado.getDepartamento() == null ? null : empleado.getDepartamento().getId(),
                empleado.getDepartamento() == null ? null : empleado.getDepartamento().getNombre(),
                empleado.getFechaIngreso(),
                empleado.getFechaEgreso(),
                empleado.getUsuario() == null ? null : empleado.getUsuario().getId(),
                empleado.getActivo(),
                empleado.getCreadoEn(),
                empleado.getActualizadoEn(),
                asignacionTurno == null ? null : asignacionTurno.getTurno().getId(),
                asignacionTurno == null ? null : asignacionTurno.getTurno().getNombre());
    }
}