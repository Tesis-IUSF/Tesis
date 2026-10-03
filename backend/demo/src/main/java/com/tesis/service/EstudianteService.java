package com.tesis.service;

import com.tesis.entity.Estudiante;
import com.tesis.entity.Usuario;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;

    public EstudianteService(EstudianteRepository estudianteRepository,
                            UsuarioRepository usuarioRepository) {
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<Estudiante> listar(Pageable pageable) {
        return estudianteRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Estudiante obtener(Integer id) {
        return buscarEstudiante(id);
    }

    public Estudiante crear(Estudiante estudiante) {
        validarEntidad(estudiante);
        if (estudianteRepository.existsByCedula(estudiante.getCedula().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula del estudiante ya está registrada");
        }
        if (estudiante.getUsuario() != null && estudiante.getUsuario().getId() != null) {
            Usuario usuario = usuarioRepository.findById(estudiante.getUsuario().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));
            estudiante.setUsuario(usuario);
        }
        return estudianteRepository.save(estudiante);
    }

    public Estudiante actualizar(Integer id, Estudiante estudiante) {
        Estudiante actual = buscarEstudiante(id);
        validarEntidad(estudiante);
        if (estudianteRepository.existsByCedula(estudiante.getCedula().trim())
                && !actual.getCedula().equalsIgnoreCase(estudiante.getCedula().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula del estudiante ya está registrada");
        }
        actual.setCedula(estudiante.getCedula().trim());
        actual.setNombre(estudiante.getNombre().trim());
        actual.setApellido(estudiante.getApellido().trim());
        actual.setFechaNacimiento(estudiante.getFechaNacimiento());
        actual.setSexo(estudiante.getSexo());
        actual.setCorreo(estudiante.getCorreo());
        actual.setTelefono(estudiante.getTelefono());
        actual.setFotoCarnetUrl(estudiante.getFotoCarnetUrl());
        actual.setNumeroExpediente(estudiante.getNumeroExpediente());
        actual.setObservaciones(estudiante.getObservaciones());
        actual.setActivo(estudiante.getActivo());
        if (estudiante.getUsuario() != null && estudiante.getUsuario().getId() != null) {
            Usuario usuario = usuarioRepository.findById(estudiante.getUsuario().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));
            actual.setUsuario(usuario);
        }
        return estudianteRepository.save(actual);
    }

    public void eliminar(Integer id) {
        if (!estudianteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }
        estudianteRepository.deleteById(id);
    }

    private Estudiante buscarEstudiante(Integer id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }

    private void validarEntidad(Estudiante estudiante) {
        if (estudiante == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La entidad es obligatoria");
        }
        if (estudiante.getCedula() == null || estudiante.getCedula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula es obligatoria");
        }
        if (estudiante.getNombre() == null || estudiante.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (estudiante.getApellido() == null || estudiante.getApellido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apellido es obligatorio");
        }
        if (estudiante.getFechaNacimiento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria");
        }
        if (estudiante.getSexo() == null || estudiante.getSexo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El sexo es obligatorio");
        }
        if (!"M".equals(estudiante.getSexo()) && !"F".equals(estudiante.getSexo())
                && !"Otro".equals(estudiante.getSexo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El sexo debe ser M, F u Otro");
        }
    }
}
