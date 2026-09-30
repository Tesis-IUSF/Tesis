package com.tesis.service;

import com.tesis.dto.InscripcionDTO.ChecklistItemResponseDTO;
import com.tesis.dto.InscripcionDTO.ChecklistUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.FormalizarInscripcionRequestDTO;
import com.tesis.dto.InscripcionDTO.InscripcionResponseDTO;
import com.tesis.entity.ChecklistMatricula;
import com.tesis.entity.Matricula;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Usuario;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class InscripcionService {

    private static final String PREINSCRITO = "preinscrito";
    private static final String EN_PROCESO = "en_proceso";
    private static final String COMPLETADA = "completada";

    private final MatriculaRepository matriculaRepository;
    private final ChecklistMatriculaRepository checklistRepository;
    private final RequisitoMatriculaRepository requisitoRepository;
    private final UsuarioRepository usuarioRepository;

    public InscripcionService(MatriculaRepository matriculaRepository,
                              ChecklistMatriculaRepository checklistRepository,
                              RequisitoMatriculaRepository requisitoRepository,
                              UsuarioRepository usuarioRepository) {
        this.matriculaRepository = matriculaRepository;
        this.checklistRepository = checklistRepository;
        this.requisitoRepository = requisitoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<InscripcionResponseDTO> listarPreinscritas() {
        return matriculaRepository.findByEstadoMatricula(PREINSCRITO).stream()
                .map(matricula -> toResponse(matricula, checklistRepository
                        .findByMatricula_Id(matricula.getId())))
                .toList();
    }

    public InscripcionResponseDTO prepararChecklist(Integer matriculaId) {
        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        validarEstadoEditable(matricula);
        List<ChecklistMatricula> checklist = inicializarChecklist(matricula);
        marcarEnProceso(matricula);
        return toResponse(matricula, checklist);
    }

    public InscripcionResponseDTO actualizarRequisito(Integer matriculaId,
                                                       Integer requisitoId,
                                                       ChecklistUpdateRequestDTO request) {
        if (request.getCumplido() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar si el requisito está cumplido");
        }

        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        validarEstadoEditable(matricula);
        inicializarChecklist(matricula);
        ChecklistMatricula item = checklistRepository
                .findByMatricula_IdAndRequisito_Id(matriculaId, requisitoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El requisito no aplica a esta matrícula"));

        item.setCumplido(request.getCumplido());
        item.setNotas(request.getNotas());
        item.setArchivoUrl(request.getArchivoUrl());
        if (Boolean.TRUE.equals(request.getCumplido())) {
            item.setFechaVerificacion(LocalDateTime.now());
            item.setVerificadoPor(buscarVerificador(request.getVerificadoPorId()));
        } else {
            item.setFechaVerificacion(null);
            item.setVerificadoPor(null);
        }
        checklistRepository.save(item);
        marcarEnProceso(matricula);
        return toResponse(matricula, checklistRepository.findByMatricula_Id(matriculaId));
    }

    public InscripcionResponseDTO formalizar(Integer matriculaId,
                                              FormalizarInscripcionRequestDTO request) {
        Matricula matricula = buscarMatriculaBloqueada(matriculaId);
        if (COMPLETADA.equals(matricula.getEstadoMatricula())) {
            return toResponse(matricula, checklistRepository.findByMatricula_Id(matriculaId));
        }
        validarEstadoEditable(matricula);

        List<ChecklistMatricula> checklist = inicializarChecklist(matricula);
        List<RequisitoMatricula> obligatorios = requisitoRepository
            .findObligatoriosActivosAplicablesAlNivel(
                matricula.getSeccion().getGrado().getNivelEducativo().getId());
        Map<Integer, ChecklistMatricula> checklistPorRequisito = checklist.stream()
                .collect(Collectors.toMap(item -> item.getRequisito().getId(),
                        Function.identity(), (first, duplicate) -> first));

        List<String> pendientes = obligatorios.stream()
                .filter(requisito -> {
                    ChecklistMatricula item = checklistPorRequisito.get(requisito.getId());
                    return item == null || !Boolean.TRUE.equals(item.getCumplido());
                })
                .map(RequisitoMatricula::getNombre)
                .toList();
        if (!pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Faltan requisitos obligatorios: " + String.join(", ", pendientes));
        }

        matricula.setEstadoMatricula(COMPLETADA);
        matricula.setFechaFormalizacion(LocalDate.now());
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
                .findActivosAplicablesAlNivel(nivelId)) {
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

    private void marcarEnProceso(Matricula matricula) {
        if (PREINSCRITO.equals(matricula.getEstadoMatricula())) {
            matricula.setEstadoMatricula(EN_PROCESO);
            matriculaRepository.save(matricula);
        }
    }

    private Usuario buscarVerificador(Integer usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Usuario verificador no encontrado"));
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
                matricula.getSeccion().getId(),
                matricula.getAnioEscolar(),
                totalObligatorios,
                obligatoriosCumplidos,
                items);
    }
}