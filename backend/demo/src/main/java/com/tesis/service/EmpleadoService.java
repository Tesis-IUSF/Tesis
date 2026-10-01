package com.tesis.service;

import com.tesis.dto.EmpleadoDTO.EmpleadoRequestDTO;
import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Empleado;
import com.tesis.entity.Usuario;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final CargoRepository cargoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           CargoRepository cargoRepository,
                           DepartamentoRepository departamentoRepository,
                           UsuarioRepository usuarioRepository) {
        this.empleadoRepository = empleadoRepository;
        this.cargoRepository = cargoRepository;
        this.departamentoRepository = departamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<EmpleadoResponseDTO> listar(Pageable pageable) {
        return empleadoRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmpleadoResponseDTO obtener(Integer id) {
        return toResponse(buscarEmpleado(id));
    }

    public EmpleadoResponseDTO crear(EmpleadoRequestDTO request) {
        if (empleadoRepository.existsByCedula(request.getCedula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        Empleado empleado = new Empleado();
        aplicarRequest(request, empleado, true);
        return toResponse(empleadoRepository.save(empleado));
    }

    public EmpleadoResponseDTO actualizar(Integer id, EmpleadoRequestDTO request) {
        Empleado empleado = buscarEmpleado(id);
        if (empleadoRepository.existsByCedulaAndIdNot(request.getCedula(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada");
        }
        aplicarRequest(request, empleado, false);
        return toResponse(empleadoRepository.save(empleado));
    }

    public void eliminar(Integer id) {
        empleadoRepository.delete(buscarEmpleado(id));
    }

    private Empleado buscarEmpleado(Integer id) {
        return empleadoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
    }

    private void aplicarRequest(EmpleadoRequestDTO request, Empleado empleado, boolean nuevo) {
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setCedula(request.getCedula());
        empleado.setCorreo(request.getCorreo());
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        empleado.setSexo(request.getSexo());
        empleado.setCargo(buscarCargo(request.getCargoId()));
        empleado.setDepartamento(buscarDepartamento(request.getDepartamentoId()));
        empleado.setFechaIngreso(request.getFechaIngreso());
        empleado.setFechaEgreso(request.getFechaEgreso());
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
        return new EmpleadoResponseDTO(
                empleado.getId(),
                empleado.getNombre(),
                empleado.getApellido(),
                empleado.getCedula(),
                empleado.getCorreo(),
                empleado.getTelefono(),
                empleado.getFechaNacimiento(),
                empleado.getSexo(),
                empleado.getCargo() == null ? null : empleado.getCargo().getId(),
                empleado.getCargo() == null ? null : empleado.getCargo().getNombreCargo(),
                empleado.getDepartamento() == null ? null : empleado.getDepartamento().getId(),
                empleado.getDepartamento() == null ? null : empleado.getDepartamento().getNombre(),
                empleado.getFechaIngreso(),
                empleado.getFechaEgreso(),
                empleado.getUsuario() == null ? null : empleado.getUsuario().getId(),
                empleado.getActivo(),
                empleado.getCreadoEn(),
                empleado.getActualizadoEn());
    }
}