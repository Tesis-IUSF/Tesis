package com.tesis.service;

import com.tesis.dto.PreinscripcionDTO.PreinscripcionRequestDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionResponseDTO;
import com.tesis.dto.PreinscripcionDTO.EstudianteData;
import com.tesis.dto.PreinscripcionDTO.RepresentanteData;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Matricula;
import com.tesis.entity.RelacionEstudianteRepresentante;
import com.tesis.entity.Representante;
import com.tesis.entity.Seccion;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.SeccionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Transactional
public class PreinscripcionService {

    private static final String ESTADO_PREINSCRITO = "preinscrito";
    private static final String ESTADO_EN_PROCESO = "en_proceso";
    private static final String ESTADO_COMPLETADA = "completada";
    private static final String ESTADO_RETIRADA = "retirada";

    private final EstudianteRepository estudianteRepository;
    private final RepresentanteRepository representanteRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final SeccionRepository seccionRepository;
    private final EdadMatriculaService edadMatriculaService;

    public PreinscripcionService(EstudianteRepository estudianteRepository,
                                 RepresentanteRepository representanteRepository,
                                 RelacionEstudianteRepresentanteRepository relacionRepository,
                                 MatriculaRepository matriculaRepository,
                                 SeccionRepository seccionRepository,
                                 EdadMatriculaService edadMatriculaService) {
        this.estudianteRepository = estudianteRepository;
        this.representanteRepository = representanteRepository;
        this.relacionRepository = relacionRepository;
        this.matriculaRepository = matriculaRepository;
        this.seccionRepository = seccionRepository;
        this.edadMatriculaService = edadMatriculaService;
    }

    public PreinscripcionResponseDTO preinscribir(PreinscripcionRequestDTO request) {
        EstudianteData datosEstudiante = request.getEstudiante();
        Estudiante estudianteExistente = estudianteRepository.findByCedulaForUpdate(datosEstudiante.getCedula())
            .orElse(null);

        Seccion seccion = seccionRepository.findByIdForUpdate(request.getSeccionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sección no encontrada"));
        if (!Boolean.TRUE.equals(seccion.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección no está activa");
        }
        if (!seccion.getAnioEscolar().equals(request.getAnioEscolar())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año escolar no corresponde a la sección seleccionada");
        }

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
        }

        Matricula matricula = buscarMatriculaReactivableOValidarNueva(
            estudiante, seccion, request.getAnioEscolar(), estudianteNuevo);

        Representante representante = guardarRepresentante(request.getRepresentante());
        guardarRelacion(request, estudiante, representante);

        LocalDate fechaSolicitud = LocalDate.now();
        if (matricula.getId() == null) {
            matricula.setEstudiante(estudiante);
            matricula.setSeccion(seccion);
            matricula.setAnioEscolar(request.getAnioEscolar());
            matricula.setTipoIngreso(estudianteNuevo ? "nuevo_ingreso" : "regular");
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
                cuposDisponibles - 1);
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
        estudiante.setCedula(datos.getCedula());
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
        Representante representante = representanteRepository.findByCedula(datos.getCedula())
                .orElseGet(Representante::new);
        representante.setCedula(datos.getCedula());
        representante.setNombre(datos.getNombre());
        representante.setApellido(datos.getApellido());
        representante.setParentesco(datos.getParentesco());
        representante.setCorreo(datos.getCorreo());
        representante.setTelefono(datos.getTelefono());
        representante.setTelefonoSecundario(datos.getTelefonoSecundario());
        representante.setDireccion(datos.getDireccion());
        representante.setOcupacion(datos.getOcupacion());
        representante.setActivo(true);
        return representanteRepository.save(representante);
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
            relacion.setFechaVinculacion(LocalDate.now());
        }
        relacion.setActivo(true);
        relacionRepository.save(relacion);
    }

    private String nombreCompleto(String nombre, String apellido) {
        return nombre + " " + apellido;
    }
}