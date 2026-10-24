package com.tesis.service;

import com.tesis.entity.Grado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;

@Service
public class EdadMatriculaService {

    public void validarElegibilidad(LocalDate fechaNacimiento, Grado grado, Short anioEscolar) {
        if (fechaNacimiento == null || grado == null || grado.getNivelEducativo() == null
                || grado.getNumeroGrado() == null || anioEscolar == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan datos para validar la edad según el grado y año escolar");
        }

        Short minima = grado.getEdadMinima();
        Short maxima = grado.getEdadMaxima();
        if (minima == null || maxima == null || minima < 0 || maxima > 120 || minima > maxima) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El grado seleccionado no tiene un rango de edad válido configurado");
        }
        LocalDate fechaCorte = LocalDate.of(anioEscolar, 12, 31);
        int edad = Period.between(fechaNacimiento, fechaCorte).getYears();
        if (edad < minima || edad > maxima) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La edad del estudiante al " + fechaCorte
                            + " debe estar entre " + minima + " y " + maxima
                            + " años para el grado seleccionado");
        }
    }
}