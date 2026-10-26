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

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           CargoRepository cargoRepository,
                           DepartamentoRepository departamentoRepository,
                           UsuarioRepository usuarioRepository,
                           AsignacionTurnoRepository asignacionTurnoRepository,
                           TurnoRepository turnoRepository,
                           CredencialQrRepository credencialQrRepository) {
        this.empleadoRepository = empleadoRepository;
        this.cargoRepository = cargoRepository;
        this.departamentoRepository = departamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.turnoRepository = turnoRepository;
        this.credencialQrRepository = credencialQrRepository;
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
    public EmpleadoResponseDTO obtener(Integer id) {
        return toResponse(buscarEmpleado(id));
    }

    public EmpleadoResponseDTO crear(EmpleadoRequestDTO request) {
        if (empleadoRepository.existsByCedula(request.getCedula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
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
        Empleado empleadoGuardado = empleadoRepository.save(empleado);

        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setPersonal(empleadoGuardado);
        asignacion.setTurno(turno);
        asignacion.setFechaDesde(empleadoGuardado.getFechaIngreso());
        AsignacionTurno asignacionGuardada = asignacionTurnoRepository.save(asignacion);
        return toResponse(empleadoGuardado, asignacionGuardada);
    }

    public EmpleadoResponseDTO actualizar(Integer id, EmpleadoRequestDTO request) {
        Empleado empleado = buscarEmpleado(id);
        if (empleadoRepository.existsByCedulaAndIdNot(request.getCedula(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        aplicarRequest(request, empleado, false);
        if (Boolean.TRUE.equals(empleado.getActivo()) && empleado.getFechaEgreso() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un empleado activo no puede tener fecha de egreso");
        }
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

    private void aplicarRequest(EmpleadoRequestDTO request, Empleado empleado, boolean nuevo) {
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setCedula(request.getCedula());
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
            return null;
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