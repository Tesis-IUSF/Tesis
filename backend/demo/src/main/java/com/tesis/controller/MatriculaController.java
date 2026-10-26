package com.tesis.controller;

import com.tesis.dto.InscripcionDTO.ChecklistUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.ChecklistBatchUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.FormalizarInscripcionRequestDTO;
import com.tesis.dto.InscripcionDTO.ExpedienteResponseDTO;
import com.tesis.dto.InscripcionDTO.InscripcionResponseDTO;
import com.tesis.dto.InscripcionDTO.MatriculaAcademicaDTO;
import com.tesis.dto.CalendarioMatriculaDTO;
import com.tesis.dto.ContinuidadDTO.EgresoPendienteResponseDTO;
import com.tesis.dto.ContinuidadDTO.GeneracionResponseDTO;
import com.tesis.dto.ContinuidadDTO.ResolverEgresoRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.AnioEscolarRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.AnioEscolarResponseDTO;
import com.tesis.dto.GestionCalendarioDTO.PeriodoRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.PeriodoResponseDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionRequestDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionResponseDTO;
import com.tesis.service.InscripcionService;
import com.tesis.service.CalendarioMatriculaService;
import com.tesis.service.PreinscripcionService;
import com.tesis.service.ConstanciaService;
import com.tesis.service.ContinuidadService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final PreinscripcionService preinscripcionService;
    private final InscripcionService inscripcionService;
    private final ConstanciaService constanciaService;
    private final CalendarioMatriculaService calendarioService;
    private final ContinuidadService continuidadService;

    public MatriculaController(PreinscripcionService preinscripcionService,
                               InscripcionService inscripcionService,
                               ConstanciaService constanciaService,
                               CalendarioMatriculaService calendarioService,
                               ContinuidadService continuidadService) {
        this.preinscripcionService = preinscripcionService;
        this.inscripcionService = inscripcionService;
        this.constanciaService = constanciaService;
        this.calendarioService = calendarioService;
        this.continuidadService = continuidadService;
    }

    @GetMapping("/calendario")
    @Cacheable(cacheNames = "calendarioMatricula",
            key = "#p0 == null ? 'activo' : #p0")
    public CalendarioMatriculaDTO obtenerCalendario(
            @RequestParam(required = false) Short anio) {
        return calendarioService.obtenerCalendario(anio);
    }

    @GetMapping("/gestion/anios")
    @Cacheable(cacheNames = "calendarioMatricula", key = "'anios'")
    public List<AnioEscolarResponseDTO> listarAniosEscolares() {
        return calendarioService.listarAnios();
    }

    @PostMapping("/gestion/anios")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    @ResponseStatus(HttpStatus.CREATED)
    public AnioEscolarResponseDTO crearAnioEscolar(
            @Valid @RequestBody AnioEscolarRequestDTO request) {
        return calendarioService.crearAnio(request);
    }

    @GetMapping("/gestion/anios/{anio}")
    @Cacheable(cacheNames = "calendarioMatricula", key = "'anio:' + #p0")
    public AnioEscolarResponseDTO obtenerAnioEscolar(@PathVariable Short anio) {
        return calendarioService.obtenerAnioGestion(anio);
    }

    @PutMapping("/gestion/anios/{anio}")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    public AnioEscolarResponseDTO actualizarAnioEscolar(
            @PathVariable Short anio,
            @Valid @RequestBody AnioEscolarRequestDTO request) {
        return calendarioService.actualizarAnio(anio, request);
    }

    @PostMapping("/gestion/anios/{anio}/activar")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    public AnioEscolarResponseDTO activarAnioEscolar(@PathVariable Short anio) {
        return calendarioService.activarAnio(anio);
    }

    @PostMapping("/gestion/anios/{anio}/continuidades")
    public GeneracionResponseDTO generarContinuidades(@PathVariable Short anio) {
        return continuidadService.generarContinuidades(anio);
    }

    @GetMapping("/gestion/anios/{anio}/egresos-pendientes")
    public List<EgresoPendienteResponseDTO> listarEgresosPendientes(@PathVariable Short anio) {
        return continuidadService.listarEgresosPendientes(anio);
    }

    @PatchMapping("/gestion/egresos/{egresoId}/resolver")
    public EgresoPendienteResponseDTO resolverEgreso(
            @PathVariable Integer egresoId,
            @Valid @RequestBody ResolverEgresoRequestDTO request,
            Principal principal) {
        return continuidadService.resolverEgreso(egresoId, request, principal.getName());
    }

    @PostMapping("/gestion/anios/{anio}/periodos")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoResponseDTO crearPeriodoMatricula(
            @PathVariable Short anio,
            @Valid @RequestBody PeriodoRequestDTO request) {
        return calendarioService.crearPeriodo(anio, request);
    }

    @PutMapping("/gestion/periodos/{id}")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    public PeriodoResponseDTO actualizarPeriodoMatricula(
            @PathVariable Integer id,
            @Valid @RequestBody PeriodoRequestDTO request) {
        return calendarioService.actualizarPeriodo(id, request);
    }

    @PatchMapping("/gestion/periodos/{id}/activar")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    public PeriodoResponseDTO activarPeriodoMatricula(@PathVariable Integer id) {
        return calendarioService.activarPeriodo(id);
    }

    @PatchMapping("/gestion/periodos/{id}/desactivar")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    public PeriodoResponseDTO desactivarPeriodoMatricula(@PathVariable Integer id) {
        return calendarioService.desactivarPeriodo(id);
    }

    @DeleteMapping("/gestion/periodos/{id}")
    @CacheEvict(cacheNames = "calendarioMatricula", allEntries = true)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPeriodoEnPreparacion(@PathVariable Integer id) {
        calendarioService.eliminarPeriodoEnPreparacion(id);
    }

    @PostMapping("/preinscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PreinscripcionResponseDTO preinscribir(
            @Valid @RequestBody PreinscripcionRequestDTO request) {
        return preinscripcionService.preinscribir(request);
    }

    @GetMapping("/{matriculaId}/preinscripcion")
    public PreinscripcionRequestDTO obtenerDatosPreinscripcion(@PathVariable Integer matriculaId) {
        return preinscripcionService.obtenerDatosPreinscripcion(matriculaId);
    }

    @PutMapping("/{matriculaId}/preinscripcion")
    public PreinscripcionResponseDTO actualizarPreinscripcion(
            @PathVariable Integer matriculaId,
            @Valid @RequestBody PreinscripcionRequestDTO request) {
        return preinscripcionService.actualizarPreinscripcion(matriculaId, request);
    }

    @GetMapping("/preinscripciones")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarPreinscritas(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(inscripcionService.listarPreinscritas(pageable));
    }

    @GetMapping("/en-proceso")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarEnProceso(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(inscripcionService.listarEnProceso(pageable));
    }

    @GetMapping("/listado")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarMatriculas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Short anioEscolar,
            @RequestParam(required = false) Integer gradoId,
            @RequestParam(required = false) Integer seccionId,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return PaginacionDTO.Respuesta.desde(
                inscripcionService.listar(
                        estado, anioEscolar, gradoId, seccionId, q, paginacion.toPageable()));
    }

    @GetMapping("/posibles-graduados")
    public PaginacionDTO.Respuesta<InscripcionResponseDTO> listarPosiblesGraduados(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer gradoId,
            @RequestParam(required = false) Integer seccionId,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return PaginacionDTO.Respuesta.desde(inscripcionService.listarPosiblesGraduados(
                gradoId, seccionId, q, paginacion.toPageable()));
    }

    @GetMapping("/{matriculaId}/checklist")
    public InscripcionResponseDTO obtenerSeguimiento(@PathVariable Integer matriculaId) {
        return inscripcionService.obtenerSeguimiento(matriculaId);
    }

    @GetMapping("/{matriculaId}/expediente")
    public ExpedienteResponseDTO obtenerExpediente(@PathVariable Integer matriculaId) {
        return inscripcionService.obtenerExpediente(matriculaId);
    }

    @GetMapping("/{matriculaId}/historial-academico")
    public List<MatriculaAcademicaDTO> obtenerHistorialAcademico(
            @PathVariable Integer matriculaId) {
        return inscripcionService.obtenerHistorialAcademico(matriculaId);
    }

    @PostMapping("/{matriculaId}/checklist")
    public InscripcionResponseDTO prepararChecklist(@PathVariable Integer matriculaId) {
        return inscripcionService.prepararChecklist(matriculaId);
    }

    @PutMapping("/{matriculaId}/checklist")
    public InscripcionResponseDTO actualizarRequisitos(
            @PathVariable Integer matriculaId,
            @Valid @RequestBody ChecklistBatchUpdateRequestDTO request,
            Principal principal) {
        return inscripcionService.actualizarRequisitos(
                matriculaId, request, principal.getName());
    }

    @PatchMapping("/{matriculaId}/checklist/{requisitoId}")
    public InscripcionResponseDTO actualizarRequisito(
            @PathVariable Integer matriculaId,
            @PathVariable Integer requisitoId,
            @Valid @RequestBody ChecklistUpdateRequestDTO request,
            Principal principal) {
        return inscripcionService.actualizarRequisito(matriculaId, requisitoId, request, principal.getName());
    }

    @PostMapping("/{matriculaId}/formalizar")
    public InscripcionResponseDTO formalizar(
            @PathVariable Integer matriculaId,
            @Valid @RequestBody(required = false) FormalizarInscripcionRequestDTO request) {
        return inscripcionService.formalizar(matriculaId, request);
    }

    @GetMapping(value = "/{matriculaId}/constancia-asignacion-cupo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarConstanciaAsignacionCupo(@PathVariable Integer matriculaId) {
        byte[] pdf = constanciaService.generarConstanciaAsignacionCupo(matriculaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename("constancia-aceptacion-cupo-" + matriculaId + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping(value = "/{matriculaId}/constancia-inscripcion", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarConstanciaInscripcion(@PathVariable Integer matriculaId) {
        byte[] pdf = constanciaService.generarConstanciaInscripcion(matriculaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("constancia-inscripcion-" + matriculaId + ".pdf").build().toString())
                .body(pdf);
    }
}