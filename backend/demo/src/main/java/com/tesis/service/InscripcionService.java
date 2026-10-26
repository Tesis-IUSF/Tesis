package com.tesis.service;

import com.tesis.dto.InscripcionDTO.ChecklistItemResponseDTO;
import com.tesis.dto.InscripcionDTO.ChecklistUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.ChecklistBatchItemRequestDTO;
import com.tesis.dto.InscripcionDTO.ChecklistBatchUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.ExpedienteResponseDTO;
import com.tesis.dto.InscripcionDTO.FormalizarInscripcionRequestDTO;
import com.tesis.dto.InscripcionDTO.InscripcionResponseDTO;
import com.tesis.dto.InscripcionDTO.EstudianteExpedienteDTO;
import com.tesis.dto.InscripcionDTO.MatriculaAcademicaDTO;
import com.tesis.dto.InscripcionDTO.RepresentanteExpedienteDTO;
import com.tesis.entity.ChecklistMatricula;
import com.tesis.entity.Matricula;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Usuario;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class InscripcionService {

    private static final String PREINSCRITO = "preinscrito";
    private static final String EN_PROCESO = "en_proceso";
    private static final String COMPLETADA = "completada";
    private static final Set<String> ESTADOS_MATRICULA = Set.of(
            PREINSCRITO, EN_PROCESO, COMPLETADA, "retirada", "anulada");

    private final MatriculaRepository matriculaRepository;
    private final ChecklistMatriculaRepository checklistRepository;
    private final RequisitoMatriculaRepository requisitoRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioMatriculaService calendarioService;
    private final Clock clock;

    public InscripcionService(MatriculaRepository matriculaRepository,
                              ChecklistMatriculaRepository checklistRepository,
                              RequisitoMatriculaRepository requisitoRepository,
                              RelacionEstudianteRepresentanteRepository relacionRepository,
                              UsuarioRepository usuarioRepository,
                              CalendarioMatriculaService calendarioService,
                              Clock clock) {
        this.matriculaRepository = matriculaRepository;
        this.checklistRepository = checklistRepository;
        this.requisitoRepository = requisitoRepository;
        this.relacionRepository = relacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendarioService = calendarioService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<InscripcionResponseDTO> listarPreinscritas(Pageable pageable) {
        return toResponses(matriculaRepository.findByEstadoMatricula(PREINSCRITO, pageable));
    }

            @Transactional(readOnly = true)
    public Page<InscripcionResponseDTO> listarEnProceso(Pageable pageable) {
        return toResponses(matriculaRepository.findByEstadoMatriculaOrderByFechaSolicitudAsc(EN_PROCESO, pageable));
    }

    @Transactional(readOnly = true)
    public Page<InscripcionResponseDTO> listar(String estado,
                                                Short anioEscolar,
                                                Integer gradoId,
                                                Integer seccionId,
                                                String busqueda,
                                                Pageable pageable) {
        String estadoNormalizado = estado == null || estado.isBlank()
                ? null : estado.trim().toLowerCase(Locale.ROOT);
        if (estadoNormalizado != null && !ESTADOS_MATRICULA.contains(estadoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado de matrícula no es válido");
        }
        String busquedaNormalizada = busqueda == null || busqueda.isBlank()
                ? null : busqueda.trim();
        return toResponses(matriculaRepository.buscarListado(
                estadoNormalizado, anioEscolar, gradoId, seccionId, busquedaNormalizada, pageable));
    }

    @Transactional(readOnly = true)
    public Page<InscripcionResponseDTO> listarPosiblesGraduados(Integer gradoId,
                                                                 Integer seccionId,
                                                                 String busqueda,
                                                                 Pageable pageable) {
        Short anioActivo = calendarioService.obtenerAnioActivo().getAnio();
        String busquedaNormalizada = busqueda == null || busqueda.isBlank()
                ? null : busqueda.trim();
        return toResponses(matriculaRepository.buscarPosiblesGraduados(
                anioActivo, gradoId, seccionId, busquedaNormalizada, pageable));
    }

    @Transactional(readOnly = true)
    public InscripcionResponseDTO obtenerSeguimiento(Integer matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        return toResponse(matricula, checklistRepository.findByMatricula_Id(matriculaId));
    }

    @Transactional(readOnly = true)
    public ExpedienteResponseDTO obtenerExpediente(Integer matriculaId) {
        Matricula matricula = matriculaRepository.buscarExpediente(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        var estudiante = matricula.getEstudiante();
        var grado = matricula.getSeccion().getGrado();
        List<RepresentanteExpedienteDTO> representantes = relacionRepository
                .findByEstudiante_IdAndActivoTrueOrderByRelacionPrincipalDesc(estudiante.getId())
                .stream()
                .map(relacion -> {
                    var representante = relacion.getRepresentante();
                    return new RepresentanteExpedienteDTO(
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
                            relacion.getRelacionPrincipal(),
                            relacion.getAutorizadoRetirar(),
                            relacion.getRecibeComunicados());
                })
                .toList();
        return new ExpedienteResponseDTO(
                matricula.getId(),
                matricula.getEstadoMatricula(),
                matricula.getAnioEscolar(),
                matricula.getFechaSolicitud(),
                matricula.getFechaFormalizacion(),
                matricula.getNumeroConstancia(),
                matricula.getTipoIngreso(),
                matricula.getInstitucionProcedencia(),
                matricula.getSeccion().getId(),
                grado.getNivelEducativo().getNombre(),
                nombreGrado(grado),
                matricula.getSeccion().getLetraSeccion(),
                new EstudianteExpedienteDTO(
                        estudiante.getId(),
                        estudiante.getCedula(),
                        estudiante.getNombre(),
                        estudiante.getApellido(),
                        estudiante.getFechaNacimiento(),
                        estudiante.getSexo(),
                        estudiante.getCorreo(),
                        estudiante.getTelefono(),
                        estudiante.getNumeroExpediente(),
                        estudiante.getObservaciones()),
                representantes);
    }

    @Transactional(readOnly = true)
    public List<MatriculaAcademicaDTO> obtenerHistorialAcademico(Integer matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        return matriculaRepository.buscarHistorialAcademico(matricula.getEstudiante().getId())
                .stream()
                .map(item -> new MatriculaAcademicaDTO(
                        item.getId(),
                        item.getAnioEscolar(),
                        item.getEstadoMatricula(),
                        item.getFechaSolicitud(),
                        item.getFechaFormalizacion(),
                        item.getSeccion().getGrado().getNivelEducativo().getNombre(),
                        nombreGrado(item.getSeccion().getGrado()),
                        item.getSeccion().getLetraSeccion()))
                .toList();
    }

    public InscripcionResponseDTO prepararChecklist(Integer matriculaId) {
        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        validarEstadoEditable(matricula);
        calendarioService.validarGestionInscripcion(
            matricula.getAnioEscolar(), matricula.getEstadoMatricula());
        List<ChecklistMatricula> checklist = inicializarChecklist(matricula);
        return toResponse(matricula, checklist);
    }

    public InscripcionResponseDTO actualizarRequisito(Integer matriculaId,
                                                       Integer requisitoId,
                                                       ChecklistUpdateRequestDTO request,
                                                       String verificadoPorEmail) {
        if (request.getCumplido() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar si el requisito está cumplido");
        }

        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        validarEstadoEditable(matricula);
        calendarioService.validarGestionInscripcion(
            matricula.getAnioEscolar(), matricula.getEstadoMatricula());
        inicializarChecklist(matricula);
        ChecklistMatricula item = checklistRepository
                .findByMatricula_IdAndRequisito_Id(matriculaId, requisitoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El requisito no aplica a esta matrícula"));

        item.setCumplido(request.getCumplido());
        item.setNotas(request.getNotas());
        item.setArchivoUrl(request.getArchivoUrl());
        if (Boolean.TRUE.equals(request.getCumplido())) {
            item.setFechaVerificacion(LocalDateTime.now(clock));
            item.setVerificadoPor(buscarUsuarioAutenticado(verificadoPorEmail));
        } else {
            item.setFechaVerificacion(null);
            item.setVerificadoPor(null);
        }
        checklistRepository.save(item);
        marcarEnProceso(matricula);
        return toResponse(matricula, checklistRepository.findByMatricula_Id(matriculaId));
    }

    public InscripcionResponseDTO actualizarRequisitos(Integer matriculaId,
                                                        ChecklistBatchUpdateRequestDTO request,
                                                        String verificadoPorEmail) {
        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        validarEstadoEditable(matricula);
        calendarioService.validarGestionInscripcion(
                matricula.getAnioEscolar(), matricula.getEstadoMatricula());
        inicializarChecklist(matricula);
        Usuario verificador = buscarUsuarioAutenticado(verificadoPorEmail);
        List<ChecklistMatricula> elementos = new ArrayList<>();
        Set<Integer> idsProcesados = new HashSet<>();

        for (ChecklistBatchItemRequestDTO cambio : request.getItems()) {
            if (!idsProcesados.add(cambio.getRequisitoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede enviar un requisito más de una vez");
            }
            ChecklistMatricula item = checklistRepository
                    .findByMatricula_IdAndRequisito_Id(
                            matriculaId, cambio.getRequisitoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "El requisito no aplica a esta matrícula"));
            item.setCumplido(cambio.getCumplido());
            item.setNotas(cambio.getNotas());
            item.setArchivoUrl(cambio.getArchivoUrl());
            if (Boolean.TRUE.equals(cambio.getCumplido())) {
                item.setFechaVerificacion(LocalDateTime.now(clock));
                item.setVerificadoPor(verificador);
            } else {
                item.setFechaVerificacion(null);
                item.setVerificadoPor(null);
            }
            elementos.add(item);
        }

        checklistRepository.saveAll(elementos);
        marcarEnProceso(matricula);
        return toResponse(
                matricula, checklistRepository.findByMatricula_Id(matriculaId));
    }

    public InscripcionResponseDTO formalizar(Integer matriculaId,
                                              FormalizarInscripcionRequestDTO request) {
        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        if (COMPLETADA.equals(matricula.getEstadoMatricula())) {
            return toResponse(matricula, checklistRepository.findByMatricula_Id(matriculaId));
        }
        validarEstadoEditable(matricula);
        calendarioService.validarGestionInscripcion(
            matricula.getAnioEscolar(), matricula.getEstadoMatricula());

        List<ChecklistMatricula> checklist = inicializarChecklist(matricula);
        List<String> pendientes = checklist.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getCumplido()))
                .map(item -> item.getRequisito().getNombre())
                .toList();
        if (!pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Faltan requisitos de inscripción: " + String.join(", ", pendientes));
        }

        matricula.setEstadoMatricula(COMPLETADA);
        matricula.setFechaFormalizacion(LocalDate.now(clock));
        if (request != null) {
            matricula.setNumeroConstancia(request.getNumeroConstancia());
            matricula.setConstanciaUrl(request.getConstanciaUrl());
        }
        matricula = matriculaRepository.save(matricula);
        return toResponse(matricula, checklist);
    }

    private Matricula buscarMatriculaBloqueada(Integer matriculaId) {
        return matriculaRepository.findByIdForUpdate(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
    }

    private void validarEstadoEditable(Matricula matricula) {
        String estado = matricula.getEstadoMatricula();
        if (!PREINSCRITO.equals(estado) && !EN_PROCESO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede gestionar una matrícula preinscrita o en proceso");
        }
    }

    private List<ChecklistMatricula> inicializarChecklist(Matricula matricula) {
        Integer nivelId = matricula.getSeccion().getGrado().getNivelEducativo().getId();
        List<ChecklistMatricula> existentes = checklistRepository
                .findByMatricula_Id(matricula.getId());
        Map<Integer, ChecklistMatricula> porRequisito = existentes.stream()
                .collect(Collectors.toMap(item -> item.getRequisito().getId(),
                        Function.identity(), (first, duplicate) -> first));

        List<ChecklistMatricula> nuevos = new ArrayList<>();
        for (RequisitoMatricula requisito : requisitoRepository
            .findActivosAplicablesAlNivel(nivelId).stream()
            .filter(item -> aplicaAlTipoIngreso(item, matricula.getTipoIngreso()))
            .toList()) {
            if (!porRequisito.containsKey(requisito.getId())) {
                ChecklistMatricula item = new ChecklistMatricula();
                item.setMatricula(matricula);
                item.setRequisito(requisito);
                item.setCumplido(false);
                nuevos.add(item);
            }
        }
        if (!nuevos.isEmpty()) {
            checklistRepository.saveAll(nuevos);
            checklistRepository.flush();
        }
        return checklistRepository.findByMatricula_Id(matricula.getId());
    }

    private boolean aplicaAlTipoIngreso(RequisitoMatricula requisito, String tipoIngreso) {
        String tiposAplicables = requisito.getAplicaTipoIngreso();
        return tiposAplicables == null || tiposAplicables.isBlank()
                || java.util.Arrays.stream(tiposAplicables.split(","))
                .map(String::trim)
                .anyMatch(tipo -> tipo.equals(tipoIngreso));
    }

    private void marcarEnProceso(Matricula matricula) {
        if (PREINSCRITO.equals(matricula.getEstadoMatricula())) {
            matricula.setEstadoMatricula(EN_PROCESO);
            matriculaRepository.save(matricula);
        }
    }

    private String nombreGrado(com.tesis.entity.Grado grado) {
        return grado.getNombreEspecial() != null && !grado.getNombreEspecial().isBlank()
                ? grado.getNombreEspecial()
                : grado.getNumeroGrado() + "° grado";
    }

    private Usuario buscarUsuarioAutenticado(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no identificado");
        }
        return usuarioRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private InscripcionResponseDTO toResponse(Matricula matricula,
                                               List<ChecklistMatricula> checklist) {
        List<ChecklistItemResponseDTO> items = checklist.stream()
                .map(item -> new ChecklistItemResponseDTO(
                        item.getId(),
                        item.getRequisito().getId(),
                        item.getRequisito().getNombre(),
                        item.getRequisito().getDescripcion(),
                        item.getRequisito().getObligatorio(),
                        item.getCumplido(),
                        item.getFechaVerificacion(),
                        item.getVerificadoPor() == null ? null : item.getVerificadoPor().getId(),
                        item.getNotas(),
                        item.getArchivoUrl()))
                .toList();
        long totalObligatorios = checklist.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequisito().getObligatorio()))
                .count();
        long obligatoriosCumplidos = checklist.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequisito().getObligatorio())
                        && Boolean.TRUE.equals(item.getCumplido()))
                .count();
        return new InscripcionResponseDTO(
                matricula.getId(),
                matricula.getEstadoMatricula(),
                matricula.getFechaSolicitud(),
                matricula.getFechaFormalizacion(),
                matricula.getNumeroConstancia(),
                matricula.getConstanciaUrl(),
                matricula.getEstudiante().getId(),
                matricula.getEstudiante().getNombre() + " "
                        + matricula.getEstudiante().getApellido(),
                matricula.getEstudiante().getCedula(),
                matricula.getSeccion().getId(),
                matricula.getAnioEscolar(),
                totalObligatorios,
                obligatoriosCumplidos,
                items);
    }

    private Page<InscripcionResponseDTO> toResponses(Page<Matricula> pagina) {
        List<Matricula> matriculas = pagina.getContent();
        if (matriculas.isEmpty()) {
            return pagina.map(matricula -> toResponse(matricula, List.of()));
        }
        Map<Integer, List<ChecklistMatricula>> checklistPorMatricula = checklistRepository
                .findByMatricula_IdIn(matriculas.stream().map(Matricula::getId).toList()).stream()
                .collect(Collectors.groupingBy(item -> item.getMatricula().getId()));
        return pagina.map(matricula -> toResponse(matricula,
            checklistPorMatricula.getOrDefault(matricula.getId(), List.of())));
    }
}