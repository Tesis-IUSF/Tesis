package com.tesis.service;

import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.Empleado;
import com.tesis.entity.Grado;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Turno;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.MatriculaRepository;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogoService {

    private static final List<String> TIPOS_INGRESO =
            List.of("nuevo_ingreso", "regular", "reingreso", "traslado");
    private static final Set<String> TIPOS_INGRESO_VALIDOS = Set.copyOf(TIPOS_INGRESO);
    private static final List<String> ESTADOS_MATRICULA_ACTIVA =
            List.of("preinscrito", "en_proceso", "completada");

    private final DepartamentoRepository departamentoRepository;
    private final CargoRepository cargoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TurnoRepository turnoRepository;
    private final RolesRepository rolesRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final RequisitoMatriculaRepository requisitoMatriculaRepository;
    private final ChecklistMatriculaRepository checklistMatriculaRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnioEscolarRepository anioEscolarRepository;

    public CatalogoService(DepartamentoRepository departamentoRepository,
                           CargoRepository cargoRepository,
                           EmpleadoRepository empleadoRepository,
                           TurnoRepository turnoRepository,
                           RolesRepository rolesRepository,
                           NivelEducativoRepository nivelEducativoRepository,
                           GradoRepository gradoRepository,
                           SeccionRepository seccionRepository,
                           RequisitoMatriculaRepository requisitoMatriculaRepository,
                           ChecklistMatriculaRepository checklistMatriculaRepository,
                           MatriculaRepository matriculaRepository,
                           AnioEscolarRepository anioEscolarRepository) {
        this.departamentoRepository = departamentoRepository;
        this.cargoRepository = cargoRepository;
        this.empleadoRepository = empleadoRepository;
        this.turnoRepository = turnoRepository;
        this.rolesRepository = rolesRepository;
        this.nivelEducativoRepository = nivelEducativoRepository;
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.requisitoMatriculaRepository = requisitoMatriculaRepository;
        this.checklistMatriculaRepository = checklistMatriculaRepository;
        this.matriculaRepository = matriculaRepository;
        this.anioEscolarRepository = anioEscolarRepository;
    }

    @Transactional(readOnly = true)
    public Page<Departamento> listarDepartamentos(Pageable pageable) {
        return departamentoRepository.findAll(pageable);
    }

    public Departamento crearDepartamento(Departamento departamento) {
        String nombre = normalizarNombreCatalogo(
                departamento != null ? departamento.getNombre() : null, "nombre");
        if (departamentoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un departamento con ese nombre");
        }
        departamento.setNombre(nombre);
        return departamentoRepository.save(departamento);
    }

    public Departamento actualizarDepartamento(Integer id, Departamento departamento) {
        Departamento actual = buscarDepartamento(id);
        String nombre = normalizarNombreCatalogo(
                departamento != null ? departamento.getNombre() : null, "nombre");
        if (departamentoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un departamento con ese nombre");
        }
        actual.setNombre(nombre);
        return departamentoRepository.save(actual);
    }

    public void eliminarDepartamento(Integer id) {
        if (!departamentoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado");
        }
        if (empleadoRepository.existsByDepartamento_Id(id)
                || cargoRepository.existsByDepartamento_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el departamento porque tiene cargos o empleados asociados");
        }
        departamentoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Cargo> listarCargos(Pageable pageable) {
        return cargoRepository.findAll(pageable);
    }

    public Cargo crearCargo(Cargo cargo) {
        String nombreCargo = normalizarNombreCatalogo(
                cargo != null ? cargo.getNombreCargo() : null, "nombre del cargo");
        Departamento departamento = buscarDepartamentoCargo(cargo);
        if (cargoRepository.existsByNombreCargoIgnoreCaseAndDepartamento_Id(
                nombreCargo, departamento.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un cargo con ese nombre en el departamento");
        }
        cargo.setNombreCargo(nombreCargo);
        cargo.setDepartamento(departamento);
        return cargoRepository.save(cargo);
    }

    public Cargo actualizarCargo(Integer id, Cargo cargo) {
        Cargo actual = buscarCargo(id);
        String nombreCargo = normalizarNombreCatalogo(
                cargo != null ? cargo.getNombreCargo() : null, "nombre del cargo");
        Departamento departamento = buscarDepartamentoCargo(cargo);
        if (cargoRepository.existsByNombreCargoIgnoreCaseAndDepartamento_IdAndIdNot(
                nombreCargo, departamento.getId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un cargo con ese nombre en el departamento");
        }
        actual.setNombreCargo(nombreCargo);
        actual.setDepartamento(departamento);
        return cargoRepository.save(actual);
    }

    public void eliminarCargo(Integer id) {
        if (!cargoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo no encontrado");
        }
        if (empleadoRepository.existsByCargo_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el cargo porque tiene empleados asociados");
        }
        cargoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Turno> listarTurnos(Pageable pageable) {
        return turnoRepository.findAll(pageable);
    }

    public Turno crearTurno(Turno turno) {
        validarTexto(turno != null ? turno.getNombre() : null, "nombre del turno");
        validarNombreTurno(turno.getNombre());
        if (turno.getHoraEntrada() == null || turno.getHoraSalida() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe indicar las horas de entrada y salida del turno");
        }
        validarMinutosTurno(turno.getToleranciaMin(), "tolerancia");
        validarMinutosTurno(turno.getMinutosSalidaAnticipadaPermitidos(),
                "minutos de salida anticipada");
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
        validarNombreTurno(turno.getNombre());
        validarMinutosTurno(turno.getToleranciaMin(), "tolerancia");
        validarMinutosTurno(turno.getMinutosSalidaAnticipadaPermitidos(),
                "minutos de salida anticipada");
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

    private void validarNombreTurno(String nombre) {
        if (nombre.trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre del turno no puede superar 100 caracteres");
        }
    }

    private void validarMinutosTurno(Short minutos, String campo) {
        if (minutos != null && (minutos < 0 || minutos > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los " + campo + " deben estar entre 0 y 180 minutos");
        }
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
        validarRangoEdad(grado);
        grado.setNivelEducativo(buscarNivelEducativo(grado.getNivelEducativo().getId()));
        return gradoRepository.save(grado);
    }

    public Grado actualizarGrado(Integer id, Grado grado) {
        Grado actual = buscarGradoForUpdate(id);
        if (grado == null || grado.getNivelEducativo() == null || grado.getNivelEducativo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el nivel educativo");
        }
        if (grado.getNumeroGrado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el número de grado");
        }
        validarRangoEdad(grado);
        NivelEducativo nivelEducativo = buscarNivelEducativo(grado.getNivelEducativo().getId());
        boolean desactivando = Boolean.TRUE.equals(actual.getActivo())
                && Boolean.FALSE.equals(grado.getActivo());
        List<Seccion> secciones = List.of();
        if (desactivando) {
            secciones = seccionRepository.findByGrado_IdForUpdate(id);
            if (tieneMatriculasActivasEnAnioActual(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede desactivar el grado porque tiene matrículas activas en el año escolar vigente");
            }
        }
        actual.setNivelEducativo(nivelEducativo);
        actual.setNumeroGrado(grado.getNumeroGrado());
        actual.setEdadMinima(grado.getEdadMinima());
        actual.setEdadMaxima(grado.getEdadMaxima());
        actual.setNombreEspecial(grado.getNombreEspecial());
        if (grado.getActivo() != null) {
            actual.setActivo(grado.getActivo());
        }
        if (desactivando) {
            secciones.forEach(seccion -> seccion.setActivo(false));
            seccionRepository.saveAll(secciones);
        }
        return gradoRepository.save(actual);
    }

    private void validarRangoEdad(Grado grado) {
        Short minima = grado.getEdadMinima();
        Short maxima = grado.getEdadMaxima();
        if (minima == null || maxima == null
                || minima < 1 || maxima < 1
                || minima > 20 || maxima > 20
                || minima > maxima) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar un rango de edad válido entre 1 y 20 años");
        }
    }

    public void eliminarGrado(Integer id) {
        buscarGradoForUpdate(id);
        List<Seccion> secciones = seccionRepository.findByGrado_IdForUpdate(id);
        if (matriculaRepository.existsBySeccion_Grado_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el grado porque tiene matrículas asociadas en el histórico");
        }
        if (!secciones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Elimine primero las secciones sin matrículas asociadas a este grado");
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

    @Transactional(readOnly = true)
    public Map<Integer, Long> contarMatriculasActivasPorSecciones(List<Integer> seccionIds) {
        if (seccionIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Long> matriculados = new HashMap<>();
        for (Object[] fila : seccionRepository.contarMatriculasActivasPorSecciones(seccionIds)) {
            matriculados.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        return matriculados;
    }

    public Seccion crearSeccion(Seccion seccion) {
        if (seccion == null || seccion.getGrado() == null || seccion.getGrado().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el grado");
        }
        if (seccion.getLetraSeccion() == null
                || !seccion.getLetraSeccion().matches("[A-Z]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La letra de la sección debe ser una letra mayúscula de la A a la Z");
        }
        if (seccion.getAnioEscolar() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        Grado grado = buscarGrado(seccion.getGrado().getId());
        if (Boolean.TRUE.equals(seccion.getActivo()) && !Boolean.TRUE.equals(grado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede activar una sección de un grado inactivo");
        }
        seccion.setGrado(grado);
        seccion.setDocentePrincipal(buscarDocente(seccion.getDocentePrincipal()));
        if (seccion.getTurno() != null && seccion.getTurno().getId() != null) {
            seccion.setTurno(buscarTurno(seccion.getTurno().getId()));
        }
        return seccionRepository.save(seccion);
    }

    public Seccion actualizarSeccion(Integer id, Seccion seccion) {
        Seccion actual = seccionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sección no encontrada"));
        if (seccion == null || seccion.getGrado() == null || seccion.getGrado().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el grado");
        }
        if (seccion.getLetraSeccion() == null
                || !seccion.getLetraSeccion().matches("[A-Z]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La letra de la sección debe ser una letra mayúscula de la A a la Z");
        }
        if (seccion.getAnioEscolar() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        Grado grado = buscarGrado(seccion.getGrado().getId());
        if (Boolean.TRUE.equals(seccion.getActivo()) && !Boolean.TRUE.equals(grado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede activar una sección de un grado inactivo");
        }
        boolean cambiandoGradoOAnio = !actual.getGrado().getId().equals(grado.getId())
                || !actual.getAnioEscolar().equals(seccion.getAnioEscolar());
        if (cambiandoGradoOAnio && matriculaRepository.existsBySeccion_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el grado o año escolar de una sección con matrículas asociadas");
        }
        boolean desactivando = Boolean.TRUE.equals(actual.getActivo())
                && Boolean.FALSE.equals(seccion.getActivo());
        if (desactivando && tieneMatriculasActivasEnAnioActual(actual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desactivar la sección porque tiene matrículas activas en el año escolar vigente");
        }
        actual.setGrado(grado);
        actual.setLetraSeccion(seccion.getLetraSeccion());
        if (seccion.getCapacidadMaxima() != null) {
            actual.setCapacidadMaxima(seccion.getCapacidadMaxima());
        }
        actual.setDocentePrincipal(resolverDocenteEnActualizacion(
                seccion.getDocentePrincipal(), actual.getDocentePrincipal()));
        actual.setTurno(seccion.getTurno() != null && seccion.getTurno().getId() != null
                ? buscarTurno(seccion.getTurno().getId()) : null);
        actual.setAnioEscolar(seccion.getAnioEscolar());
        if (seccion.getActivo() != null) {
            actual.setActivo(seccion.getActivo());
        }
        return seccionRepository.save(actual);
    }

    public void eliminarSeccion(Integer id) {
        seccionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sección no encontrada"));
        if (matriculaRepository.existsBySeccion_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la sección porque tiene matrículas asociadas en el histórico");
        }
        seccionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Empleado> listarDocentesActivos() {
        return empleadoRepository.findActivosPorNombreCargo("Docente");
    }

    private Empleado buscarDocente(Empleado docente) {
        if (docente == null || docente.getId() == null) {
            return null;
        }
        Empleado empleado = empleadoRepository.findById(docente.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Docente no encontrado"));
        if (!Boolean.TRUE.equals(empleado.getActivo())
                || empleado.getCargo() == null
                || empleado.getCargo().getNombreCargo() == null
                || !"Docente".equalsIgnoreCase(empleado.getCargo().getNombreCargo().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El personal asignado debe estar activo y tener el cargo Docente");
        }
        return empleado;
    }

    private Empleado resolverDocenteEnActualizacion(Empleado solicitado, Empleado actual) {
        if (solicitado == null || solicitado.getId() == null) {
            return null;
        }
        if (actual != null && solicitado.getId().equals(actual.getId())) {
            return actual;
        }
        return buscarDocente(solicitado);
    }

    private boolean tieneMatriculasActivasEnAnioActual(Integer gradoId) {
        return anioEscolarRepository.findFirstByActivoTrue()
                .map(AnioEscolar::getAnio)
                .map(anio -> matriculaRepository
                        .existsBySeccion_Grado_IdAndAnioEscolarAndEstadoMatriculaIn(
                                gradoId, anio, ESTADOS_MATRICULA_ACTIVA))
                .orElse(false);
    }

    private boolean tieneMatriculasActivasEnAnioActual(Seccion seccion) {
        return anioEscolarRepository.findFirstByActivoTrue()
                .map(AnioEscolar::getAnio)
                .filter(anio -> anio.equals(seccion.getAnioEscolar()))
                .map(anio -> matriculaRepository
                        .existsBySeccion_IdAndAnioEscolarAndEstadoMatriculaIn(
                                seccion.getId(), anio, ESTADOS_MATRICULA_ACTIVA))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<RequisitoMatricula> listarRequisitosMatricula(Pageable pageable) {
        return requisitoMatriculaRepository.findAll(pageable);
    }

    public RequisitoMatricula crearRequisitoMatricula(RequisitoMatricula requisitoMatricula) {
        validarTexto(requisitoMatricula != null ? requisitoMatricula.getNombre() : null, "nombre del requisito");
        requisitoMatricula.setAplicaTipoIngreso(
            normalizarTiposIngreso(requisitoMatricula.getAplicaTipoIngreso()));
        if (requisitoMatricula.getNivelEducativo() != null && requisitoMatricula.getNivelEducativo().getId() != null) {
            buscarNivelEducativo(requisitoMatricula.getNivelEducativo().getId());
        }
        return requisitoMatriculaRepository.save(requisitoMatricula);
    }

    public RequisitoMatricula actualizarRequisitoMatricula(Integer id, RequisitoMatricula requisitoMatricula) {
        RequisitoMatricula actual = buscarRequisitoMatricula(id);
        validarTexto(requisitoMatricula != null ? requisitoMatricula.getNombre() : null, "nombre del requisito");
        String tiposAplicables = normalizarTiposIngreso(requisitoMatricula.getAplicaTipoIngreso());
        if (requisitoMatricula.getNivelEducativo() != null && requisitoMatricula.getNivelEducativo().getId() != null) {
            buscarNivelEducativo(requisitoMatricula.getNivelEducativo().getId());
        }
        actual.setNombre(requisitoMatricula.getNombre());
        actual.setDescripcion(requisitoMatricula.getDescripcion());
        actual.setObligatorio(requisitoMatricula.getObligatorio());
        actual.setAplicaTipoIngreso(tiposAplicables);
        actual.setNivelEducativo(requisitoMatricula.getNivelEducativo());
        actual.setDocumentoTemplateUrl(requisitoMatricula.getDocumentoTemplateUrl());
        actual.setActivo(requisitoMatricula.getActivo());
        return requisitoMatriculaRepository.save(actual);
    }

    private String normalizarTiposIngreso(String tipos) {
        if (tipos == null || tipos.isBlank()) {
            return null;
        }
        List<String> proporcionados = Arrays.stream(tipos.split(",", -1))
                .map(String::trim)
                .map(tipo -> tipo.toLowerCase(Locale.ROOT))
                .toList();
        Set<String> seleccionados = Set.copyOf(proporcionados);
        if (seleccionados.size() != proporcionados.size()
                || !TIPOS_INGRESO_VALIDOS.containsAll(seleccionados)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los tipos de ingreso deben ser nuevo_ingreso, regular, reingreso o traslado");
        }
        return TIPOS_INGRESO.stream()
                .filter(seleccionados::contains)
                .collect(Collectors.joining(","));
    }

    public void eliminarRequisitoMatricula(Integer id) {
        if (!requisitoMatriculaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisito no encontrado");
        }
        if (checklistMatriculaRepository.existsByRequisito_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El requisito ya se usó; desactívalo");
        }
        requisitoMatriculaRepository.deleteById(id);
    }

    private Departamento buscarDepartamento(Integer id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado"));
    }

    private Departamento buscarDepartamentoCargo(Cargo cargo) {
        if (cargo == null || cargo.getDepartamento() == null
                || cargo.getDepartamento().getId() == null
                || cargo.getDepartamento().getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un departamento para el cargo");
        }
        return departamentoRepository.findById(cargo.getDepartamento().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El departamento seleccionado no existe"));
    }

    private String normalizarNombreCatalogo(String valor, String campo) {
        validarTexto(valor, campo);
        String normalizado = valor.trim();
        if (normalizado.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " no puede superar 100 caracteres");
        }
        return normalizado;
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

    private Grado buscarGradoForUpdate(Integer id) {
        return gradoRepository.findByIdForUpdate(id)
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
