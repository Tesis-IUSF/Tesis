package com.tesis.service;

import com.tesis.entity.Matricula;
import com.tesis.entity.RetiroMatricula;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RetiroMatriculaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ConstanciaService {

    private final MatriculaRepository matriculaRepository;
    private final RetiroMatriculaRepository retiroRepository;
    private final ConstanciaPdfGenerator pdfGenerator;

    public ConstanciaService(MatriculaRepository matriculaRepository,
                             RetiroMatriculaRepository retiroRepository,
                             ConstanciaPdfGenerator pdfGenerator) {
        this.matriculaRepository = matriculaRepository;
        this.retiroRepository = retiroRepository;
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] generarConstanciaInscripcion(Integer matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
        if (matricula.getFechaFormalizacion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La constancia solo está disponible para matrículas formalizadas");
        }
        return pdfGenerator.generarInscripcion(matricula);
    }

    public byte[] generarConstanciaRetiro(Integer matriculaId, Integer retiroId) {
        RetiroMatricula retiro = retiroRepository.findById(retiroId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Retiro no encontrado"));
        if (!retiro.getMatricula().getId().equals(matriculaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El retiro no pertenece a la matrícula indicada");
        }
        return pdfGenerator.generarRetiro(retiro);
    }
}