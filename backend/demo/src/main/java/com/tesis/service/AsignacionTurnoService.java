package com.tesis.service;

import com.tesis.dto.AsignacionTurnoDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AsignacionTurnoService {

    private final AsignacionTurnoRepository asignacionTurnoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TurnoRepository turnoRepository;

    public AsignacionTurnoService(AsignacionTurnoRepository asignacionTurnoRepository,
                                  EmpleadoRepository empleadoRepository,
                                  TurnoRepository turnoRepository) {
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.turnoRepository = turnoRepository;
    }

    public AsignacionTurnoDTO.Response asignar(AsignacionTurnoDTO.Request request) {
        if (request.getFechaHasta() != null && request.getFechaHasta().isBefore(request.getFechaDesde())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha hasta no puede ser anterior a la fecha desde");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(request.getEmpleadoId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        Turno turno = turnoRepository.findById(request.getTurnoId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (!Boolean.TRUE.equals(turno.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un turno inactivo");
        }
        if (asignacionTurnoRepository.existeSolapamiento(empleado.getId(), request.getFechaDesde(),
                request.getFechaHasta())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado ya tiene una asignación de turno en ese rango de fechas");
        }

        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setPersonal(empleado);
        asignacion.setTurno(turno);
        asignacion.setFechaDesde(request.getFechaDesde());
        asignacion.setFechaHasta(request.getFechaHasta());
        return toResponse(asignacionTurnoRepository.save(asignacion));
    }

    public AsignacionTurnoDTO.Response reemplazarVigente(Integer empleadoId,
                                                          AsignacionTurnoDTO.CambioRequest request) {
        Empleado empleado = empleadoRepository.findByIdForUpdate(empleadoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        Turno nuevoTurno = turnoRepository.findById(request.getTurnoId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (!Boolean.TRUE.equals(nuevoTurno.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un turno inactivo");
        }

        List<AsignacionTurno> vigentes = asignacionTurnoRepository
                .buscarVigentesEnFecha(empleadoId, request.getFechaDesde());
        if (vigentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado no tiene una asignación vigente en esa fecha");
        }
        if (vigentes.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado tiene varias asignaciones vigentes; deben corregirse antes del cambio");
        }

        AsignacionTurno vigente = vigentes.getFirst();
        if (vigente.getTurno().getId().equals(nuevoTurno.getId())) {
            return toResponse(vigente);
        }
        if (asignacionTurnoRepository.existeSolapamientoExcepto(empleadoId, request.getFechaDesde(), null,
                vigente.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existe otra asignación futura que se solapa con el nuevo turno");
        }

        if (vigente.getFechaDesde().isBefore(request.getFechaDesde())) {
            vigente.setFechaHasta(request.getFechaDesde().minusDays(1));
            asignacionTurnoRepository.save(vigente);
            AsignacionTurno nueva = new AsignacionTurno();
            nueva.setPersonal(empleado);
            nueva.setTurno(nuevoTurno);
            nueva.setFechaDesde(request.getFechaDesde());
            return toResponse(asignacionTurnoRepository.save(nueva));
        }

        vigente.setTurno(nuevoTurno);
        return toResponse(asignacionTurnoRepository.save(vigente));
    }

    @Transactional(readOnly = true)
    public List<AsignacionTurnoDTO.Response> listarPorEmpleado(Integer empleadoId) {
        if (!empleadoRepository.existsById(empleadoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado");
        }
        return asignacionTurnoRepository.buscarPorEmpleado(empleadoId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AsignacionTurnoDTO.Response toResponse(AsignacionTurno asignacion) {
        Empleado empleado = asignacion.getPersonal();
        Turno turno = asignacion.getTurno();
        return new AsignacionTurnoDTO.Response(
                asignacion.getId(), empleado.getId(), empleado.getNombre() + " " + empleado.getApellido(),
                turno.getId(), turno.getNombre(), asignacion.getFechaDesde(), asignacion.getFechaHasta());
    }
}