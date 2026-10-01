package com.tesis.service;

import com.tesis.entity.Grado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

@Service
public class EdadMatriculaService {

    public void validarElegibilidad(LocalDate fechaNacimiento, Grado grado, Short anioEscolar) {
        if (fechaNacimiento == null || grado == null || grado.getNivelEducativo() == null
                || grado.getNumeroGrado() == null || anioEscolar == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan datos para validar la edad según el grado y año escolar");
        }

        RangoEdad rango = obtenerRango(grado);
        LocalDate fechaCorte = LocalDate.of(anioEscolar, 12, 31);
        int edad = Period.between(fechaNacimiento, fechaCorte).getYears();
        if (edad < rango.minima() || edad > rango.maxima()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La edad del estudiante al " + fechaCorte
                            + " debe estar entre " + rango.minima() + " y " + rango.maxima()
                            + " años para el grado seleccionado");
        }
    }

    private RangoEdad obtenerRango(Grado grado) {
        String nivel = normalizar(grado.getNivelEducativo().getNombre());
        int numeroGrado = grado.getNumeroGrado();

        if (nivel.startsWith("educacion primaria") || nivel.equals("primaria")) {
            if (numeroGrado >= 1 && numeroGrado <= 6) {
                int edadMinima = 6 + numeroGrado - 1;
                return new RangoEdad(edadMinima, edadMinima + 1);
            }
        } else if (nivel.contains("media general")) {
            if (numeroGrado >= 1 && numeroGrado <= 5) {
                int edadMinima = 12 + numeroGrado - 1;
                return new RangoEdad(edadMinima, edadMinima + 1);
            }
        } else if (nivel.contains("media tecnica")) {
            if (numeroGrado >= 1 && numeroGrado <= 5) {
                int edadMinima = 12 + numeroGrado - 1;
                return new RangoEdad(edadMinima, edadMinima + 1);
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No hay una regla de edad configurada para el nivel y grado seleccionados");
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private record RangoEdad(int minima, int maxima) {
    }
}