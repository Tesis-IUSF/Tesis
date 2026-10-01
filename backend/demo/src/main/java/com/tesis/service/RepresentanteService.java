package com.tesis.service;

import com.tesis.entity.Representante;
import com.tesis.repository.RepresentanteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RepresentanteService {

    private final RepresentanteRepository representanteRepository;

    public RepresentanteService(RepresentanteRepository representanteRepository) {
        this.representanteRepository = representanteRepository;
    }

    @Transactional(readOnly = true)
    public Page<Representante> listar(Pageable pageable) {
        return representanteRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Representante obtener(Integer id) {
        return buscarRepresentante(id);
    }

    public Representante crear(Representante representante) {
        validarEntidad(representante);
        if (representanteRepository.existsByCedula(representante.getCedula().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula del representante ya está registrada");
        }
        return representanteRepository.save(representante);
    }

    public Representante actualizar(Integer id, Representante representante) {
        Representante actual = buscarRepresentante(id);
        validarEntidad(representante);
        if (representanteRepository.existsByCedula(representante.getCedula().trim())
                && !actual.getCedula().equalsIgnoreCase(representante.getCedula().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula del representante ya está registrada");
        }
        actual.setCedula(representante.getCedula().trim());
        actual.setNombre(representante.getNombre().trim());
        actual.setApellido(representante.getApellido().trim());
        actual.setParentesco(representante.getParentesco());
        actual.setCorreo(representante.getCorreo());
        actual.setTelefono(representante.getTelefono());
        actual.setTelefonoSecundario(representante.getTelefonoSecundario());
        actual.setDireccion(representante.getDireccion());
        actual.setOcupacion(representante.getOcupacion());
        actual.setActivo(representante.getActivo());
        return representanteRepository.save(actual);
    }

    public void eliminar(Integer id) {
        if (!representanteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Representante no encontrado");
        }
        representanteRepository.deleteById(id);
    }

    private Representante buscarRepresentante(Integer id) {
        return representanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Representante no encontrado"));
    }

    private void validarEntidad(Representante representante) {
        if (representante == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La entidad es obligatoria");
        }
        if (representante.getCedula() == null || representante.getCedula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula es obligatoria");
        }
        if (representante.getNombre() == null || representante.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (representante.getApellido() == null || representante.getApellido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apellido es obligatorio");
        }
        if (representante.getCorreo() == null || representante.getCorreo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
    }
}
