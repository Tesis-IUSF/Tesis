package com.tesis.service;

import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Grado;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Turno;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.NivelEducativoRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.repository.TurnoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CatalogoService {

    private final DepartamentoRepository departamentoRepository;
    private final CargoRepository cargoRepository;
    private final TurnoRepository turnoRepository;
    private final RolesRepository rolesRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final RequisitoMatriculaRepository requisitoMatriculaRepository;

    public CatalogoService(DepartamentoRepository departamentoRepository,
                           CargoRepository cargoRepository,
                           TurnoRepository turnoRepository,
                           RolesRepository rolesRepository,
                           NivelEducativoRepository nivelEducativoRepository,
                           GradoRepository gradoRepository,
                           SeccionRepository seccionRepository,
                           RequisitoMatriculaRepository requisitoMatriculaRepository) {
        this.departamentoRepository = departamentoRepository;
        this.cargoRepository = cargoRepository;
        this.turnoRepository = turnoRepository;
        this.rolesRepository = rolesRepository;
        this.nivelEducativoRepository = nivelEducativoRepository;
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.requisitoMatriculaRepository = requisitoMatriculaRepository;
    }

    @Transactional(readOnly = true)
    public Page<Departamento> listarDepartamentos(Pageable pageable) {
        return departamentoRepository.findAll(pageable);
    }

    public Departamento crearDepartamento(Departamento departamento) {
        validarTexto(departamento != null ? departamento.getNombre() : null, "nombre");
        return departamentoRepository.save(departamento);
    }

    public Departamento actualizarDepartamento(Integer id, Departamento departamento) {
        Departamento actual = buscarDepartamento(id);
        validarTexto(departamento != null ? departamento.getNombre() : null, "nombre");
        actual.setNombre(departamento.getNombre());
        return departamentoRepository.save(actual);
    }

    public void eliminarDepartamento(Integer id) {
        if (!departamentoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado");
        }
        departamentoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Cargo> listarCargos(Pageable pageable) {
        return cargoRepository.findAll(pageable);
    }

    public Cargo crearCargo(Cargo cargo) {
        validarTexto(cargo != null ? cargo.getNombreCargo() : null, "nombre del cargo");
        return cargoRepository.save(cargo);
    }

    public Cargo actualizarCargo(Integer id, Cargo cargo) {
        Cargo actual = buscarCargo(id);
        validarTexto(cargo != null ? cargo.getNombreCargo() : null, "nombre del cargo");
        actual.setNombreCargo(cargo.getNombreCargo());
        actual.setDepartamento(cargo.getDepartamento());
        return cargoRepository.save(actual);
    }

    public void eliminarCargo(Integer id) {
        if (!cargoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado");
        }
        cargoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Turno> listarTurnos(Pageable pageable) {
        return turnoRepository.findAll(pageable);
    }

    public Turno crearTurno(Turno turno) {
        validarTexto(turno != null ? turno.getNombre() : null, "nombre del turno");
        if (turno.getHoraEntrada() == null || turno.getHoraSalida() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe indicar las horas de entrada y salida del turno");
        }
        turno.setNombre(turno.getNombre().trim());
        turno.setToleranciaMin(turno.getToleranciaMin() == null ? (short) 0 : turno.getToleranciaMin());
        turno.setMinutosSalidaAnticipadaPermitidos(turno.getMinutosSalidaAnticipadaPermitidos() == null
            ? (short) 0 : turno.getMinutosSalidaAnticipadaPermitidos());
        turno.setRequiereJustificacionTardanza(turno.getRequiereJustificacionTardanza() == null
            ? true : turno.getRequiereJustificacionTardanza());
        turno.setLunes(turno.getLunes() == null || turno.getLunes());
        turno.setMartes(turno.getMartes() == null || turno.getMartes());
        turno.setMiercoles(turno.getMiercoles() == null || turno.getMiercoles());
        turno.setJueves(turno.getJueves() == null || turno.getJueves());
        turno.setViernes(turno.getViernes() == null || turno.getViernes());
        turno.setSabado(Boolean.TRUE.equals(turno.getSabado()));
        turno.setDomingo(Boolean.TRUE.equals(turno.getDomingo()));
        turno.setActivo(turno.getActivo() == null || turno.getActivo());
        return turnoRepository.save(turno);
    }

    public Turno actualizarTurno(Integer id, Turno turno) {
        Turno actual = buscarTurno(id);
        validarTexto(turno != null ? turno.getNombre() : null, "nombre del turno");
        actual.setNombre(turno.getNombre().trim());
        if (turno.getHoraEntrada() != null) actual.setHoraEntrada(turno.getHoraEntrada());
        if (turno.getHoraSalida() != null) actual.setHoraSalida(turno.getHoraSalida());
        if (turno.getToleranciaMin() != null) actual.setToleranciaMin(turno.getToleranciaMin());
        if (turno.getMinutosSalidaAnticipadaPermitidos() != null) {
            actual.setMinutosSalidaAnticipadaPermitidos(turno.getMinutosSalidaAnticipadaPermitidos());
        }
        if (turno.getRequiereJustificacionTardanza() != null) {
            actual.setRequiereJustificacionTardanza(turno.getRequiereJustificacionTardanza());
        }
        if (turno.getLunes() != null) actual.setLunes(turno.getLunes());
        if (turno.getMartes() != null) actual.setMartes(turno.getMartes());
        if (turno.getMiercoles() != null) actual.setMiercoles(turno.getMiercoles());
        if (turno.getJueves() != null) actual.setJueves(turno.getJueves());
        if (turno.getViernes() != null) actual.setViernes(turno.getViernes());
        if (turno.getSabado() != null) actual.setSabado(turno.getSabado());
        if (turno.getDomingo() != null) actual.setDomingo(turno.getDomingo());
        if (turno.getActivo() != null) actual.setActivo(turno.getActivo());
        return turnoRepository.save(actual);
    }

    public void eliminarTurno(Integer id) {
        if (!turnoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
        turnoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Roles> listarRoles(Pageable pageable) {
        return rolesRepository.findAll(pageable);
    }

    public Roles crearRol(Roles roles) {
        validarTexto(roles != null ? roles.getNombreRol() : null, "nombre del rol");
        if (rolesRepository.findByNombreRol(roles.getNombreRol().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre");
        }
        return rolesRepository.save(roles);
    }

    public Roles actualizarRol(Integer id, Roles roles) {
        Roles actual = buscarRol(id);
        validarTexto(roles != null ? roles.getNombreRol() : null, "nombre del rol");
        String nombre = roles.getNombreRol().trim();
        rolesRepository.findByNombreRol(nombre)
                .filter(rol -> !rol.getId().equals(id))
                .ifPresent(rol -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre");
                });
        actual.setNombreRol(nombre);
        actual.setDescripcion(roles.getDescripcion());
        actual.setActivo(roles.getActivo());
        return rolesRepository.save(actual);
    }

    public void eliminarRol(Integer id) {
        if (!rolesRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
        }
        rolesRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<NivelEducativo> listarNivelesEducativos(Pageable pageable) {
        return nivelEducativoRepository.findAll(pageable);
    }

    public NivelEducativo crearNivelEducativo(NivelEducativo nivelEducativo) {
        validarTexto(nivelEducativo != null ? nivelEducativo.getNombre() : null, "nombre del nivel");
        if (nivelEducativoRepository.findByNombreIgnoreCase(nivelEducativo.getNombre().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un nivel educativo con ese nombre");
        }
        return nivelEducativoRepository.save(nivelEducativo);
    }

    public NivelEducativo actualizarNivelEducativo(Integer id, NivelEducativo nivelEducativo) {
        NivelEducativo actual = buscarNivelEducativo(id);
        validarTexto(nivelEducativo != null ? nivelEducativo.getNombre() : null, "nombre del nivel");
        String nombre = nivelEducativo.getNombre().trim();
        nivelEducativoRepository.findByNombreIgnoreCase(nombre)
                .filter(nivel -> !nivel.getId().equals(id))
                .ifPresent(nivel -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un nivel educativo con ese nombre");
                });
        actual.setNombre(nombre);
        actual.setDescripcion(nivelEducativo.getDescripcion());
        actual.setDuracionAnios(nivelEducativo.getDuracionAnios());
        actual.setOrdinal(nivelEducativo.getOrdinal());
        actual.setActivo(nivelEducativo.getActivo());
        return nivelEducativoRepository.save(actual);
    }

    public void eliminarNivelEducativo(Integer id) {
        if (!nivelEducativoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nivel educativo no encontrado");
        }
        nivelEducativoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Grado> listarGrados(Pageable pageable) {
        return gradoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Grado> listarGradosPorNivel(Integer nivelEducativoId, Pageable pageable) {
        return gradoRepository.findByNivelEducativo_Id(nivelEducativoId, pageable);
    }

    public Grado crearGrado(Grado grado) {
        if (grado == null || grado.getNivelEducativo() == null || grado.getNivelEducativo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el nivel educativo");
        }
        if (grado.getNumeroGrado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el número de grado");
        }
        buscarNivelEducativo(grado.getNivelEducativo().getId());
        return gradoRepository.save(grado);
    }

    public Grado actualizarGrado(Integer id, Grado grado) {
        Grado actual = buscarGrado(id);
        if (grado == null || grado.getNivelEducativo() == null || grado.getNivelEducativo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el nivel educativo");
        }
        if (grado.getNumeroGrado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el número de grado");
        }
        buscarNivelEducativo(grado.getNivelEducativo().getId());
        actual.setNivelEducativo(grado.getNivelEducativo());
        actual.setNumeroGrado(grado.getNumeroGrado());
        actual.setNombreEspecial(grado.getNombreEspecial());
        actual.setActivo(grado.getActivo());
        return gradoRepository.save(actual);
    }

    public void eliminarGrado(Integer id) {
        if (!gradoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado");
        }
        gradoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Seccion> listarSecciones(Pageable pageable) {
        return seccionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Seccion> listarSeccionesPorGrado(Integer gradoId, Pageable pageable) {
        return seccionRepository.findByGrado_Id(gradoId, pageable);
    }

    public Seccion crearSeccion(Seccion seccion) {
        if (seccion == null || seccion.getGrado() == null || seccion.getGrado().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el grado");
        }
        if (seccion.getLetraSeccion() == null || seccion.getLetraSeccion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar la letra de la sección");
        }
        if (seccion.getAnioEscolar() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        buscarGrado(seccion.getGrado().getId());
        return seccionRepository.save(seccion);
    }

    public Seccion actualizarSeccion(Integer id, Seccion seccion) {
        Seccion actual = buscarSeccion(id);
        if (seccion == null || seccion.getGrado() == null || seccion.getGrado().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el grado");
        }
        if (seccion.getLetraSeccion() == null || seccion.getLetraSeccion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar la letra de la sección");
        }
        if (seccion.getAnioEscolar() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        buscarGrado(seccion.getGrado().getId());
        actual.setGrado(seccion.getGrado());
        actual.setLetraSeccion(seccion.getLetraSeccion());
        actual.setCapacidadMaxima(seccion.getCapacidadMaxima());
        actual.setDocentePrincipal(seccion.getDocentePrincipal());
        actual.setTurno(seccion.getTurno());
        actual.setAnioEscolar(seccion.getAnioEscolar());
        actual.setActivo(seccion.getActivo());
        return seccionRepository.save(actual);
    }

    public void eliminarSeccion(Integer id) {
        if (!seccionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada");
        }
        seccionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<RequisitoMatricula> listarRequisitosMatricula(Pageable pageable) {
        return requisitoMatriculaRepository.findAll(pageable);
    }

    public RequisitoMatricula crearRequisitoMatricula(RequisitoMatricula requisitoMatricula) {
        validarTexto(requisitoMatricula != null ? requisitoMatricula.getNombre() : null, "nombre del requisito");
        if (requisitoMatricula.getNivelEducativo() != null && requisitoMatricula.getNivelEducativo().getId() != null) {
            buscarNivelEducativo(requisitoMatricula.getNivelEducativo().getId());
        }
        return requisitoMatriculaRepository.save(requisitoMatricula);
    }

    public RequisitoMatricula actualizarRequisitoMatricula(Integer id, RequisitoMatricula requisitoMatricula) {
        RequisitoMatricula actual = buscarRequisitoMatricula(id);
        validarTexto(requisitoMatricula != null ? requisitoMatricula.getNombre() : null, "nombre del requisito");
        if (requisitoMatricula.getNivelEducativo() != null && requisitoMatricula.getNivelEducativo().getId() != null) {
            buscarNivelEducativo(requisitoMatricula.getNivelEducativo().getId());
        }
        actual.setNombre(requisitoMatricula.getNombre());
        actual.setDescripcion(requisitoMatricula.getDescripcion());
        actual.setObligatorio(requisitoMatricula.getObligatorio());
        actual.setNivelEducativo(requisitoMatricula.getNivelEducativo());
        actual.setDocumentoTemplateUrl(requisitoMatricula.getDocumentoTemplateUrl());
        actual.setActivo(requisitoMatricula.getActivo());
        return requisitoMatriculaRepository.save(actual);
    }

    public void eliminarRequisitoMatricula(Integer id) {
        if (!requisitoMatriculaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisito no encontrado");
        }
        requisitoMatriculaRepository.deleteById(id);
    }

    private Departamento buscarDepartamento(Integer id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado"));
    }

    private Cargo buscarCargo(Integer id) {
        return cargoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado"));
    }

    private Turno buscarTurno(Integer id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    }

    private Roles buscarRol(Integer id) {
        return rolesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    }

    private NivelEducativo buscarNivelEducativo(Integer id) {
        return nivelEducativoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nivel educativo no encontrado"));
    }

    private Grado buscarGrado(Integer id) {
        return gradoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado"));
    }

    private Seccion buscarSeccion(Integer id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    private RequisitoMatricula buscarRequisitoMatricula(Integer id) {
        return requisitoMatriculaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisito no encontrado"));
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + campo + " es obligatorio");
        }
    }
}
