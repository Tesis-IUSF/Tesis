package com.tesis.service;

import com.tesis.dto.RetiroDTO.HistorialRetirosResponseDTO;
import com.tesis.dto.RetiroDTO.RetiroRequestDTO;
import com.tesis.dto.RetiroDTO.RetiroResponseDTO;
import com.tesis.entity.Matricula;
import com.tesis.entity.Representante;
import com.tesis.entity.RetiroMatricula;
import com.tesis.entity.Usuario;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.RetiroMatriculaRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class RetiroService {

    private static final String ESTADO_COMPLETADA = "completada";
    private static final String ESTADO_RETIRADA = "retirada";

    private final MatriculaRepository matriculaRepository;
    private final RetiroMatriculaRepository retiroRepository;
    private final RepresentanteRepository representanteRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final UsuarioRepository usuarioRepository;
        private final Clock clock;

    public RetiroService(MatriculaRepository matriculaRepository,
                         RetiroMatriculaRepository retiroRepository,
                         RepresentanteRepository representanteRepository,
                         RelacionEstudianteRepresentanteRepository relacionRepository,
                         UsuarioRepository usuarioRepository,
                         Clock clock) {
        this.matriculaRepository = matriculaRepository;
        this.retiroRepository = retiroRepository;
        this.representanteRepository = representanteRepository;
        this.relacionRepository = relacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

        public RetiroResponseDTO retirar(Integer matriculaId, RetiroRequestDTO request, String procesadoPorEmail) {
                if (request == null || request.getFechaRetiro() == null
                                || request.getMotivo() == null || request.getMotivo().isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "La fecha y el motivo del retiro son obligatorios");
                }
                if (request.getFechaRetiro().isAfter(LocalDate.now(clock))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "La fecha de retiro no puede ser futura");
                }
        Matricula matricula = matriculaRepository.findByIdForUpdate(matriculaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Matrícula no encontrada"));
        if (!ESTADO_COMPLETADA.equals(matricula.getEstadoMatricula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede retirar oficialmente a un estudiante con matrícula completada");
        }
        if (matricula.getFechaFormalizacion() != null
                && request.getFechaRetiro().isBefore(matricula.getFechaFormalizacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de retiro no puede ser anterior a la formalización de la matrícula");
        }

        Representante representante = buscarRepresentanteAutorizado(
                matricula, request.getRepresentanteId());
        validarSolicitante(request, representante);
        Usuario procesadoPor = buscarUsuarioAutenticado(procesadoPorEmail);

        RetiroMatricula retiro = new RetiroMatricula();
        retiro.setMatricula(matricula);
        retiro.setRepresentante(representante);
        retiro.setSolicitanteNombre(representante == null
                ? request.getSolicitanteNombre().trim() : representante.getNombre());
        retiro.setSolicitanteApellido(representante == null
                ? request.getSolicitanteApellido().trim() : representante.getApellido());
        retiro.setSolicitanteCedula(representante == null
                ? request.getSolicitanteCedula().trim() : representante.getCedula());
        retiro.setMotivo(request.getMotivo().trim());
        retiro.setFechaRetiro(request.getFechaRetiro());
        retiro.setProcesadoPor(procesadoPor);
        retiro = retiroRepository.saveAndFlush(retiro);

        matricula.setEstadoMatricula(ESTADO_RETIRADA);
        matriculaRepository.save(matricula);
        return toResponse(retiro);
    }

    @Transactional(readOnly = true)
    public HistorialRetirosResponseDTO obtenerHistorial(Integer matriculaId) {
        if (!matriculaRepository.existsById(matriculaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada");
        }
        List<RetiroResponseDTO> retiros = retiroRepository
                .findByMatricula_IdOrderByFechaRetiroDesc(matriculaId).stream()
                .map(this::toResponse)
                .toList();
        return new HistorialRetirosResponseDTO(matriculaId, retiros);
    }

    private Representante buscarRepresentanteAutorizado(Matricula matricula,
                                                         Integer representanteId) {
        if (representanteId == null) {
            return null;
        }
        Representante representante = representanteRepository.findById(representanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Representante no encontrado"));
        if (!Boolean.TRUE.equals(representante.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El representante está inactivo");
        }
        boolean autorizado = relacionRepository
                .findByEstudiante_IdAndRepresentante_IdAndActivoTrueAndAutorizadoRetirarTrue(
                        matricula.getEstudiante().getId(), representanteId)
                .isPresent();
        if (!autorizado) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El representante no está autorizado para solicitar el retiro");
        }
        return representante;
    }

    private void validarSolicitante(RetiroRequestDTO request, Representante representante) {
        if (representante == null
                && (esVacio(request.getSolicitanteNombre())
                || esVacio(request.getSolicitanteApellido())
                || esVacio(request.getSolicitanteCedula()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para un solicitante externo se requieren nombres, apellidos y cédula");
        }
    }

        private Usuario buscarUsuarioAutenticado(String email) {
                if (email == null || email.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no identificado");
                }
                return usuarioRepository.findByEmail(email).orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private RetiroResponseDTO toResponse(RetiroMatricula retiro) {
        Matricula matricula = retiro.getMatricula();
        return new RetiroResponseDTO(
                retiro.getId(),
                matricula.getId(),
                matricula.getEstudiante().getId(),
                matricula.getEstudiante().getNombre() + " "
                        + matricula.getEstudiante().getApellido(),
                matricula.getAnioEscolar(),
                matricula.getEstadoMatricula(),
                retiro.getFechaRetiro(),
                retiro.getMotivo(),
                retiro.getRepresentante() == null ? null : retiro.getRepresentante().getId(),
                retiro.getSolicitanteNombre(),
                retiro.getSolicitanteApellido(),
                retiro.getSolicitanteCedula(),
                retiro.getProcesadoPor() == null ? null : retiro.getProcesadoPor().getId(),
                retiro.getCreadoEn());
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}