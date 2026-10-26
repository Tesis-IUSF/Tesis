package com.tesis.service;

import com.tesis.dto.PreinscripcionDTO.PreinscripcionRequestDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionResponseDTO;
import com.tesis.dto.PreinscripcionDTO.EstudianteData;
import com.tesis.dto.PreinscripcionDTO.RepresentanteData;
import com.tesis.entity.ChecklistMatricula;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Matricula;
import com.tesis.entity.RelacionEstudianteRepresentante;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Representante;
import com.tesis.entity.Seccion;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.validation.CedulaEstudiantil;
import com.tesis.validation.CedulaVenezolana;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PreinscripcionService {

    private static final String ESTADO_PREINSCRITO = "preinscrito";
    private static final String ESTADO_EN_PROCESO = "en_proceso";
    private static final String ESTADO_COMPLETADA = "completada";
    private static final String ESTADO_RETIRADA = "retirada";
    private static final String TIPO_NUEVO_INGRESO = "nuevo_ingreso";
    private static final String TIPO_REGULAR = "regular";
    private static final String TIPO_REINGRESO = "reingreso";
    private static final String TIPO_TRASLADO = "traslado";

    private final EstudianteRepository estudianteRepository;
    private final RepresentanteRepository representanteRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final SeccionRepository seccionRepository;
    private final ChecklistMatriculaRepository checklistRepository;
    private final RequisitoMatriculaRepository requisitoRepository;
    private final EdadMatriculaService edadMatriculaService;
    private final CalendarioMatriculaService calendarioService;
    private final Clock clock;

    public PreinscripcionService(EstudianteRepository estudianteRepository,
                                 RepresentanteRepository representanteRepository,
                                 RelacionEstudianteRepresentanteRepository relacionRepository,
                                 MatriculaRepository matriculaRepository,
                                 SeccionRepository seccionRepository,
                                 ChecklistMatriculaRepository checklistRepository,
                                 RequisitoMatriculaRepository requisitoRepository,
                                 EdadMatriculaService edadMatriculaService,
                                 CalendarioMatriculaService calendarioService,
                                 Clock clock) {
        this.estudianteRepository = estudianteRepository;
        this.representanteRepository = representanteRepository;
        this.relacionRepository = relacionRepository;
        this.matriculaRepository = matriculaRepository;
        this.seccionRepository = seccionRepository;
        this.checklistRepository = checklistRepository;
        this.requisitoRepository = requisitoRepository;
        this.edadMatriculaService = edadMatriculaService;
        this.calendarioService = calendarioService;
        this.clock = clock;
    }

    public PreinscripcionResponseDTO preinscribir(PreinscripcionRequestDTO request) {
        calendarioService.validarAnioExistente(request.getAnioEscolar());
        EstudianteData datosEstudiante = request.getEstudiante();
        String cedulaRepresentante =
                normalizarCedulaRepresentante(request.getRepresentante());
        datosEstudiante.setCedula(resolverCedulaNueva(datosEstudiante, cedulaRepresentante));
        Estudiante estudianteExistente = estudianteRepository
                .findByCedulaForUpdate(datosEstudiante.getCedula()).orElse(null);

        Seccion seccion = seccionRepository.findByIdForUpdate(request.getSeccionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sección no encontrada"));
        if (!Boolean.TRUE.equals(seccion.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección no está activa");
        }
        if (!Boolean.TRUE.equals(seccion.getGrado().getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El grado no está activo");
        }
        if (!seccion.getAnioEscolar().equals(request.getAnioEscolar())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año escolar no corresponde a la sección seleccionada");
        }
        calendarioService.validarPreinscripcion(request.getAnioEscolar());

        LocalDate fechaNacimiento = estudianteExistente == null
            ? datosEstudiante.getFechaNacimiento() : estudianteExistente.getFechaNacimiento();
        edadMatriculaService.validarElegibilidad(fechaNacimiento, seccion.getGrado(), request.getAnioEscolar());

        long cuposDisponibles = seccionRepository.calcularCuposDisponibles(seccion.getId())
                .orElse(0L);
        if (cuposDisponibles <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La sección no tiene cupos disponibles");
        }

        boolean estudianteNuevo = estudianteExistente == null;
        Estudiante estudiante = estudianteExistente;
        if (estudianteNuevo) {
            try {
                estudiante = estudianteRepository.saveAndFlush(crearEstudiante(datosEstudiante));
            } catch (DataIntegrityViolationException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un estudiante con esa cédula", exception);
            }
        } else if (datosEstudiante.getFotoCarnetUrl() != null
                && !datosEstudiante.getFotoCarnetUrl().isBlank()) {
            estudiante.setFotoCarnetUrl(datosEstudiante.getFotoCarnetUrl());
            estudiante = estudianteRepository.save(estudiante);
        }

        Matricula matricula = buscarMatriculaReactivableOValidarNueva(
            estudiante, seccion, request.getAnioEscolar(), estudianteNuevo);

        Representante representante = guardarRepresentante(request.getRepresentante());
        guardarRelacion(request, estudiante, representante);

        LocalDate fechaSolicitud = LocalDate.now(clock);
        if (matricula.getId() == null) {
            matricula.setEstudiante(estudiante);
            matricula.setSeccion(seccion);
            matricula.setAnioEscolar(request.getAnioEscolar());
                matricula.setTipoIngreso(determinarTipoIngreso(estudiante,
                    request.getAnioEscolar(), request.getInstitucionProcedencia()));
                matricula.setInstitucionProcedencia(normalizarInstitucionProcedencia(
                    request.getInstitucionProcedencia()));
        }
        matricula.setFechaSolicitud(fechaSolicitud);
        matricula.setFechaFormalizacion(null);
        matricula.setNumeroConstancia(null);
        matricula.setConstanciaUrl(null);
        matricula.setEstadoMatricula(ESTADO_PREINSCRITO);
        matricula = matriculaRepository.saveAndFlush(matricula);

        return new PreinscripcionResponseDTO(
                matricula.getId(),
                matricula.getEstadoMatricula(),
                matricula.getFechaSolicitud(),
                estudiante.getId(),
                nombreCompleto(estudiante.getNombre(), estudiante.getApellido()),
                representante.getId(),
                nombreCompleto(representante.getNombre(), representante.getApellido()),
                seccion.getId(),
                matricula.getAnioEscolar(),
                cuposDisponibles - 1,
                matricula.getTipoIngreso(),
                estudiante.getCedula());
    }

    @Transactional(readOnly = true)
    public PreinscripcionRequestDTO obtenerDatosPreinscripcion(Integer matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        Estudiante estudiante = matricula.getEstudiante();
        RelacionEstudianteRepresentante relacion = relacionRepository
                .findByEstudiante_IdAndActivoTrue(estudiante.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getRelacionPrincipal()))
                .findFirst()
                .orElseGet(() -> relacionRepository
                        .findByEstudiante_IdAndActivoTrue(estudiante.getId()).stream()
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "La matrícula no tiene un representante activo asociado")));
        Representante representante = relacion.getRepresentante();

        EstudianteData datosEstudiante = new EstudianteData(
                estudiante.getCedula(),
                !esCedulaEscolar(estudiante.getCedula()),
                null,
                null,
                estudiante.getNombre(),
                estudiante.getApellido(),
                estudiante.getFechaNacimiento(),
                estudiante.getSexo(),
                estudiante.getCorreo(),
                estudiante.getTelefono(),
                estudiante.getFotoCarnetUrl(),
                estudiante.getNumeroExpediente());
        RepresentanteData datosRepresentante = new RepresentanteData(
                representante.getId(),
                representante.getCedula(),
                representante.getNombre(),
                representante.getApellido(),
                representante.getParentesco(),
                representante.getCorreo(),
                representante.getTelefono(),
                representante.getTelefonoSecundario(),
                representante.getDireccion(),
                representante.getOcupacion(),
                representante.getFotoUrl());

        return new PreinscripcionRequestDTO(
                datosEstudiante,
                datosRepresentante,
                matricula.getSeccion().getId(),
                matricula.getAnioEscolar(),
                matricula.getInstitucionProcedencia(),
                relacion.getRelacionPrincipal(),
                relacion.getAutorizadoRetirar(),
                relacion.getRecibeComunicados());
    }

    public PreinscripcionResponseDTO actualizarPreinscripcion(
            Integer matriculaId,
            PreinscripcionRequestDTO request) {
        Matricula matricula = matriculaRepository.findByIdForUpdate(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        if (!ESTADO_PREINSCRITO.equals(matricula.getEstadoMatricula())
                && !ESTADO_EN_PROCESO.equals(matricula.getEstadoMatricula())
                && !ESTADO_COMPLETADA.equals(matricula.getEstadoMatricula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden editar matrículas preinscritas, en proceso o completadas");
        }
        if (!Objects.equals(matricula.getAnioEscolar(), request.getAnioEscolar())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año escolar de la matrícula no se puede cambiar");
        }

        Estudiante estudiante = matricula.getEstudiante();
        request.getEstudiante().setCedula(resolverCedulaActualizada(
                request.getEstudiante(), estudiante, request.getRepresentante()));
        normalizarCedulaRepresentante(request.getRepresentante());

        Seccion nuevaSeccion = seccionRepository.findByIdForUpdate(request.getSeccionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (!Boolean.TRUE.equals(nuevaSeccion.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección no está activa");
        }
        if (!Objects.equals(nuevaSeccion.getAnioEscolar(), matricula.getAnioEscolar())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La sección debe pertenecer al mismo año escolar de la matrícula");
        }

        EstudianteData datosEstudiante = request.getEstudiante();
        edadMatriculaService.validarElegibilidad(
                datosEstudiante.getFechaNacimiento(),
                nuevaSeccion.getGrado(),
                matricula.getAnioEscolar());

        boolean cambioSeccion = !matricula.getSeccion().getId().equals(nuevaSeccion.getId());
        if (cambioSeccion) {
            Matricula existente = matriculaRepository
                    .findByEstudiante_IdAndAnioEscolarAndSeccion_Id(
                            estudiante.getId(), matricula.getAnioEscolar(), nuevaSeccion.getId())
                    .orElse(null);
            if (existente != null && !existente.getId().equals(matriculaId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estudiante ya tiene una matrícula para esta sección y año escolar");
            }
            if (seccionRepository.calcularCuposDisponibles(nuevaSeccion.getId()).orElse(0L) <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La sección no tiene cupos disponibles");
            }
        }

        Integer nivelAnteriorId = matricula.getSeccion().getGrado()
                .getNivelEducativo().getId();
        boolean formalizada = ESTADO_COMPLETADA.equals(matricula.getEstadoMatricula());
        String tipoIngresoAnterior = matricula.getTipoIngreso();
        String institucionAnterior = matricula.getInstitucionProcedencia();
        actualizarEstudiante(estudiante, datosEstudiante);
        try {
            estudianteRepository.saveAndFlush(estudiante);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe otro estudiante con esa cédula", exception);
        }

        Representante representante = actualizarRepresentante(request.getRepresentante());
        actualizarRelacion(request, estudiante, representante);

        matricula.setSeccion(nuevaSeccion);
        String institucionNueva = normalizarInstitucionProcedencia(
                request.getInstitucionProcedencia());
        if (!formalizada) {
            matricula.setInstitucionProcedencia(institucionNueva);
        }
        if (!formalizada && !Objects.equals(institucionAnterior, institucionNueva)) {
            matricula.setTipoIngreso(determinarTipoIngreso(
                    estudiante, matricula.getAnioEscolar(), institucionNueva));
        }
        try {
            matriculaRepository.saveAndFlush(matricula);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante ya tiene una matrícula para esa sección y año escolar",
                    exception);
        }

        Integer nivelNuevoId = nuevaSeccion.getGrado().getNivelEducativo().getId();
        if (!formalizada && (!Objects.equals(nivelAnteriorId, nivelNuevoId)
                || !Objects.equals(tipoIngresoAnterior, matricula.getTipoIngreso()))) {
            sincronizarChecklist(matricula);
        }

        long cuposDisponibles = seccionRepository.calcularCuposDisponibles(nuevaSeccion.getId())
                .orElse(0L);
        return new PreinscripcionResponseDTO(
                matricula.getId(),
                matricula.getEstadoMatricula(),
                matricula.getFechaSolicitud(),
                estudiante.getId(),
                nombreCompleto(estudiante.getNombre(), estudiante.getApellido()),
                representante.getId(),
                nombreCompleto(representante.getNombre(), representante.getApellido()),
                nuevaSeccion.getId(),
                matricula.getAnioEscolar(),
                cuposDisponibles,
                matricula.getTipoIngreso(),
                estudiante.getCedula());
    }

    private void actualizarEstudiante(Estudiante estudiante, EstudianteData datos) {
        estudiante.setCedula(CedulaVenezolana.esValida(datos.getCedula())
                ? CedulaVenezolana.normalizarFormato(datos.getCedula())
                : CedulaEstudiantil.normalizar(datos.getCedula()));
        estudiante.setNombre(datos.getNombre());
        estudiante.setApellido(datos.getApellido());
        estudiante.setFechaNacimiento(datos.getFechaNacimiento());
        estudiante.setSexo(datos.getSexo());
        estudiante.setCorreo(datos.getCorreo());
        estudiante.setTelefono(datos.getTelefono());
        estudiante.setFotoCarnetUrl(datos.getFotoCarnetUrl());
        estudiante.setNumeroExpediente(datos.getNumeroExpediente());
    }

    private Representante actualizarRepresentante(RepresentanteData datos) {
        validarTelefonoRepresentante(datos);
        String cedulaOriginal = datos.getCedula().trim();
        String cedula = normalizarCedulaRepresentante(datos);
        Representante representante = buscarRepresentanteParaActualizacion(datos, cedula, cedulaOriginal);
        representante.setCedula(cedula);
        representante.setNombre(datos.getNombre());
        representante.setApellido(datos.getApellido());
        representante.setParentesco(datos.getParentesco());
        representante.setCorreo(datos.getCorreo());
        representante.setTelefono(datos.getTelefono().trim());
        representante.setTelefonoSecundario(datos.getTelefonoSecundario() == null
                ? null : datos.getTelefonoSecundario().trim());
        representante.setDireccion(datos.getDireccion());
        representante.setOcupacion(datos.getOcupacion());
        representante.setFotoUrl(datos.getFotoUrl());
        representante.setActivo(true);
        try {
            return representanteRepository.saveAndFlush(representante);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe otro representante con esa cédula", exception);
        }
    }

    private void actualizarRelacion(PreinscripcionRequestDTO request,
                                   Estudiante estudiante,
                                   Representante representante) {
        List<RelacionEstudianteRepresentante> relaciones =
                relacionRepository.findByEstudiante_IdAndActivoTrue(estudiante.getId());
        relaciones.stream()
                .filter(item -> !item.getRepresentante().getId().equals(representante.getId())
                        || !Boolean.TRUE.equals(request.getRelacionPrincipal()))
                .forEach(item -> item.setRelacionPrincipal(false));
        RelacionEstudianteRepresentante relacion = relaciones.stream()
                .filter(item -> item.getRepresentante().getId().equals(representante.getId()))
                .findFirst()
                .orElseGet(() -> relacionRepository
                        .findByEstudiante_IdAndRepresentante_Id(
                                estudiante.getId(), representante.getId())
                        .orElseGet(RelacionEstudianteRepresentante::new));
        relacion.setEstudiante(estudiante);
        relacion.setRepresentante(representante);
        relacion.setRelacionPrincipal(Boolean.TRUE.equals(request.getRelacionPrincipal()));
        relacion.setAutorizadoRetirar(Boolean.TRUE.equals(request.getAutorizadoRetirar()));
        relacion.setRecibeComunicados(Boolean.TRUE.equals(request.getRecibeComunicados()));
        if (relacion.getFechaVinculacion() == null) {
            relacion.setFechaVinculacion(LocalDate.now(clock));
        }
        relacion.setActivo(true);
        relacionRepository.save(relacion);
    }

    private void sincronizarChecklist(Matricula matricula) {
        Integer nivelId = matricula.getSeccion().getGrado().getNivelEducativo().getId();
        List<RequisitoMatricula> aplicables = requisitoRepository
                .findActivosAplicablesAlNivel(nivelId).stream()
                .filter(requisito -> aplicaAlTipoIngreso(requisito, matricula.getTipoIngreso()))
                .toList();
        Map<Integer, ChecklistMatricula> existentes = checklistRepository
                .findByMatricula_Id(matricula.getId()).stream()
                .collect(Collectors.toMap(
                        item -> item.getRequisito().getId(), Function.identity()));
        Map<Integer, RequisitoMatricula> requisitosActuales = aplicables.stream()
                .collect(Collectors.toMap(RequisitoMatricula::getId, Function.identity()));
        List<ChecklistMatricula> obsoletos = existentes.entrySet().stream()
                .filter(entry -> !requisitosActuales.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!obsoletos.isEmpty()) {
            checklistRepository.deleteAll(obsoletos);
            checklistRepository.flush();
        }

        List<ChecklistMatricula> nuevos = new ArrayList<>();
        for (RequisitoMatricula requisito : aplicables) {
            if (!existentes.containsKey(requisito.getId())) {
                ChecklistMatricula item = new ChecklistMatricula();
                item.setMatricula(matricula);
                item.setRequisito(requisito);
                item.setCumplido(false);
                nuevos.add(item);
            }
        }
        if (!nuevos.isEmpty()) {
            checklistRepository.saveAll(nuevos);
        }
    }

    private boolean aplicaAlTipoIngreso(RequisitoMatricula requisito, String tipoIngreso) {
        String tiposAplicables = requisito.getAplicaTipoIngreso();
        return tiposAplicables == null || tiposAplicables.isBlank()
                || java.util.Arrays.stream(tiposAplicables.split(","))
                .map(String::trim)
                .anyMatch(tipo -> tipo.equals(tipoIngreso));
    }

    private String determinarTipoIngreso(Estudiante estudiante,
                                         Short anioEscolar,
                                         String institucionProcedencia) {
        if (institucionProcedencia != null && !institucionProcedencia.isBlank()) {
            return TIPO_TRASLADO;
        }

        Short anioAnterior = (short) (anioEscolar - 1);
        if (matriculaRepository.existsByEstudiante_IdAndAnioEscolarAndEstadoMatriculaIn(
                estudiante.getId(), anioAnterior,
                List.of(ESTADO_EN_PROCESO, ESTADO_COMPLETADA))) {
            return TIPO_REGULAR;
        }
        if (matriculaRepository.tieneHistorialNoAnuladoAnteriorA(
                estudiante.getId(), anioEscolar)) {
            return TIPO_REINGRESO;
        }
        return TIPO_NUEVO_INGRESO;
    }

    private String normalizarInstitucionProcedencia(String institucionProcedencia) {
        if (institucionProcedencia == null || institucionProcedencia.isBlank()) {
            return null;
        }
        return institucionProcedencia.trim();
    }

    private Matricula buscarMatriculaReactivableOValidarNueva(Estudiante estudiante,
                                                               Seccion seccion,
                                                               Short anioEscolar,
                                                               boolean estudianteNuevo) {
        if (estudianteNuevo) {
            return new Matricula();
        }

        var matriculaMismoGrupo = matriculaRepository
                .findByEstudiante_IdAndAnioEscolarAndSeccion_Id(
                        estudiante.getId(), anioEscolar, seccion.getId());
        if (matriculaMismoGrupo.isPresent()) {
            Matricula matricula = matriculaMismoGrupo.get();
            if (ESTADO_RETIRADA.equals(matricula.getEstadoMatricula())) {
                return matricula;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante ya tiene una matrícula para esta sección y año escolar");
        }

        boolean tieneMatriculaActivaEnElAnio = matriculaRepository
                .findByEstudiante_IdAndAnioEscolar(estudiante.getId(), anioEscolar)
                .stream()
                .anyMatch(matricula -> ESTADO_PREINSCRITO.equals(matricula.getEstadoMatricula())
                        || ESTADO_EN_PROCESO.equals(matricula.getEstadoMatricula())
                        || ESTADO_COMPLETADA.equals(matricula.getEstadoMatricula()));
        if (tieneMatriculaActivaEnElAnio) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante ya tiene una matrícula activa para este año escolar");
        }
        return new Matricula();
    }

    private Estudiante crearEstudiante(EstudianteData datos) {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(CedulaEstudiantil.normalizar(datos.getCedula()));
        estudiante.setNombre(datos.getNombre());
        estudiante.setApellido(datos.getApellido());
        estudiante.setFechaNacimiento(datos.getFechaNacimiento());
        estudiante.setSexo(datos.getSexo());
        estudiante.setCorreo(datos.getCorreo());
        estudiante.setTelefono(datos.getTelefono());
        estudiante.setFotoCarnetUrl(datos.getFotoCarnetUrl());
        estudiante.setNumeroExpediente(datos.getNumeroExpediente());
        estudiante.setActivo(true);
        return estudiante;
    }

    private Representante guardarRepresentante(RepresentanteData datos) {
        validarTelefonoRepresentante(datos);
        String cedulaOriginal = datos.getCedula().trim();
        String cedula = normalizarCedulaRepresentante(datos);
        Representante representante;
        if (datos.getId() != null) {
            representante = buscarRepresentanteSeleccionado(datos.getId());
        } else {
            if (representanteRepository.findByCedula(cedula)
                    .or(() -> representanteRepository.findByCedula(cedulaOriginal)).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La cédula ya pertenece a un representante. Búscalo y selecciónalo.");
            }
            representante = new Representante();
        }
        representante.setCedula(cedula);
        representante.setNombre(datos.getNombre());
        representante.setApellido(datos.getApellido());
        representante.setParentesco(datos.getParentesco());
        representante.setCorreo(datos.getCorreo());
        representante.setTelefono(datos.getTelefono().trim());
        representante.setTelefonoSecundario(datos.getTelefonoSecundario() == null
                ? null : datos.getTelefonoSecundario().trim());
        representante.setDireccion(datos.getDireccion());
        representante.setOcupacion(datos.getOcupacion());
        if (datos.getFotoUrl() != null && !datos.getFotoUrl().isBlank()) {
            representante.setFotoUrl(datos.getFotoUrl());
        }
        representante.setActivo(true);
        try {
            return representanteRepository.saveAndFlush(representante);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe otro representante con esa cédula", exception);
        }
    }

    private Representante buscarRepresentanteParaActualizacion(RepresentanteData datos,
                                                                String cedula,
                                                                String cedulaOriginal) {
        if (datos.getId() != null) {
            return buscarRepresentanteSeleccionado(datos.getId());
        }
        return representanteRepository.findByCedula(cedula)
                .or(() -> representanteRepository.findByCedula(cedulaOriginal))
                .orElseGet(Representante::new);
    }

    private void validarTelefonoRepresentante(RepresentanteData datos) {
        if (datos == null || datos.getTelefono() == null || datos.getTelefono().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El teléfono principal del representante es obligatorio");
        }
    }

    private Representante buscarRepresentanteSeleccionado(Integer id) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El representante seleccionado no existe"));
        if (!Boolean.TRUE.equals(representante.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede seleccionar un representante inactivo");
        }
        return representante;
    }

    private void guardarRelacion(PreinscripcionRequestDTO request,
                                 Estudiante estudiante,
                                 Representante representante) {
        RelacionEstudianteRepresentante relacion = relacionRepository
                .findByEstudiante_IdAndRepresentante_Id(estudiante.getId(), representante.getId())
                .orElseGet(RelacionEstudianteRepresentante::new);
        relacion.setEstudiante(estudiante);
        relacion.setRepresentante(representante);
        relacion.setRelacionPrincipal(Boolean.TRUE.equals(request.getRelacionPrincipal()));
        relacion.setAutorizadoRetirar(Boolean.TRUE.equals(request.getAutorizadoRetirar()));
        relacion.setRecibeComunicados(Boolean.TRUE.equals(request.getRecibeComunicados()));
        if (relacion.getFechaVinculacion() == null) {
            relacion.setFechaVinculacion(LocalDate.now(clock));
        }
        relacion.setActivo(true);
        relacionRepository.save(relacion);
    }

    private String nombreCompleto(String nombre, String apellido) {
        return nombre + " " + apellido;
    }

    private String normalizarCedulaRepresentante(RepresentanteData datos) {
        if (datos == null || datos.getCedula() == null || datos.getCedula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cédula del representante es obligatoria");
        }
        String cedula = CedulaVenezolana.normalizarFormato(datos.getCedula());
        if (!CedulaVenezolana.esValida(cedula)) {
            String cedulaLegada = datos.getCedula().trim();
            if (representanteRepository.existsByCedula(cedulaLegada)) {
                datos.setCedula(cedulaLegada);
                return cedulaLegada;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    CedulaVenezolana.MENSAJE);
        }
        datos.setCedula(cedula);
        return cedula;
    }

    private String resolverCedulaNueva(EstudianteData datos, String cedulaRepresentante) {
        String cedulaEnviada = datos.getCedula();
        if (Boolean.TRUE.equals(datos.getTieneCedulaIdentidad())) {
            String cedulaIdentidad = CedulaVenezolana.normalizarFormato(cedulaEnviada);
            if (CedulaVenezolana.esValida(cedulaIdentidad)) {
                return cedulaIdentidad;
            }
            if (cedulaEnviada != null && estudianteRepository.existsByCedula(cedulaEnviada.trim())) {
                return cedulaEnviada.trim();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CedulaVenezolana.MENSAJE);
        }
        if (cedulaEnviada != null && !cedulaEnviada.isBlank()
                && estudianteRepository.existsByCedula(cedulaEnviada.trim())) {
            return cedulaEnviada.trim();
        }

        return generarCedulaEscolar(datos, cedulaRepresentante);
    }

    private String resolverCedulaActualizada(EstudianteData datos,
                                              Estudiante estudiante,
                                              RepresentanteData representante) {
        String cedulaActual = estudiante.getCedula();
        if (Boolean.TRUE.equals(datos.getTieneCedulaIdentidad())) {
            String cedulaIdentidad = CedulaVenezolana.normalizarFormato(datos.getCedula());
            if (!CedulaVenezolana.esValida(cedulaIdentidad)) {
                if (datos.getCedula() != null
                        && datos.getCedula().trim().equalsIgnoreCase(cedulaActual)) {
                    return cedulaActual;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        CedulaVenezolana.MENSAJE);
            }
            validarCedulaDisponible(cedulaIdentidad, estudiante.getId());
            return cedulaIdentidad;
        }

        if (esCedulaEscolar(cedulaActual)) {
            return cedulaActual;
        }
        String cedulaRepresentante = normalizarCedulaRepresentante(representante);
        String generada = generarCedulaEscolar(datos, cedulaRepresentante);
        validarCedulaDisponible(generada, estudiante.getId());
        return generada;
    }

    private String generarCedulaEscolar(EstudianteData datos, String cedulaRepresentante) {
        if (datos.getNacionalidad() == null || datos.getNacionalidad().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nacionalidad del estudiante es obligatoria para generar su cédula escolar");
        }
        if (datos.getOrdenNacimiento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El orden de nacimiento es obligatorio para generar la cédula escolar");
        }

        String generada;
        try {
            generada = CedulaEstudiantil.generar(
                    datos.getNacionalidad(),
                    datos.getOrdenNacimiento(),
                    datos.getFechaNacimiento(),
                    cedulaRepresentante);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    exception.getMessage(), exception);
        }
        return generada;
    }

    private boolean esCedulaEscolar(String cedula) {
        return cedula != null && cedula.regionMatches(true, 0, "CE-", 0, 3);
    }

    private void validarCedulaDisponible(String cedula, Integer estudianteId) {
        estudianteRepository.findByCedula(cedula)
                .filter(otro -> !otro.getId().equals(estudianteId))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "La cédula ya está registrada por otro estudiante");
                });
    }
}