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
import com.tesis.entity.Usuario;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Transactional
public class PreinscripcionService {

    private static final String ESTADO_PREINSCRITO = "preinscrito";

    private final EstudianteRepository estudianteRepository;
    private final RepresentanteRepository representanteRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final SeccionRepository seccionRepository;
    private final UsuarioRepository usuarioRepository;

    public PreinscripcionService(EstudianteRepository estudianteRepository,
                                 RepresentanteRepository representanteRepository,
                                 RelacionEstudianteRepresentanteRepository relacionRepository,
                                 MatriculaRepository matriculaRepository,
                                 SeccionRepository seccionRepository,
                                 UsuarioRepository usuarioRepository) {
        this.estudianteRepository = estudianteRepository;
        this.representanteRepository = representanteRepository;
        this.relacionRepository = relacionRepository;
        this.matriculaRepository = matriculaRepository;
        this.seccionRepository = seccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PreinscripcionResponseDTO preinscribir(PreinscripcionRequestDTO request) {
        EstudianteData datosEstudiante = request.getEstudiante();
        if (estudianteRepository.existsByCedula(datosEstudiante.getCedula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un estudiante con esa cédula");
        }

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

        long cuposDisponibles = seccionRepository.calcularCuposDisponibles(seccion.getId())
                .orElse(0L);
        if (cuposDisponibles <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La sección no tiene cupos disponibles");
        }

        Usuario usuario = buscarUsuario(datosEstudiante.getUsuarioId());
        Estudiante estudiante = crearEstudiante(datosEstudiante, usuario);
        estudiante = estudianteRepository.save(estudiante);

        Representante representante = guardarRepresentante(request.getRepresentante());
        guardarRelacion(request, estudiante, representante);

        LocalDate fechaSolicitud = LocalDate.now();
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar(request.getAnioEscolar());
        matricula.setFechaSolicitud(fechaSolicitud);
        matricula.setTipoIngreso("nuevo_ingreso");
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

    private Estudiante crearEstudiante(EstudianteData datos, Usuario usuario) {
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
        estudiante.setUsuario(usuario);
        estudiante.setActivo(true);
        return estudiante;
    }

    private Usuario buscarUsuario(Integer usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        if (estudianteRepository.existsByUsuario_Id(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario ya está vinculado a otro estudiante");
        }
        return usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));
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