package com.tesis.service;

import com.tesis.entity.Grado;
import com.tesis.entity.NivelEducativo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EdadMatriculaServiceTest {

    private final EdadMatriculaService service = new EdadMatriculaService();

    @Test
    void aceptaLimitesDePrimariaEnFechaDeCorte() {
        assertDoesNotThrow(() -> service.validarElegibilidad(
                LocalDate.of(2020, 12, 31), crearGrado("Educación Primaria", 1), (short) 2026));
        assertDoesNotThrow(() -> service.validarElegibilidad(
                LocalDate.of(2019, 1, 1), crearGrado("Educación Primaria", 1), (short) 2026));
    }

    @Test
    void rechazaEdadFueraDelRangoRegular() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarElegibilidad(
                        LocalDate.of(2021, 1, 1), crearGrado("Educación Primaria", 1), (short) 2026));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
        void aplicaRangosDeMediaGeneralYTecnicaHastaQuintoAnio() {
        assertDoesNotThrow(() -> service.validarElegibilidad(
                LocalDate.of(2014, 12, 31), crearGrado("Educación Media General", 1), (short) 2026));
        assertDoesNotThrow(() -> service.validarElegibilidad(
            LocalDate.of(2009, 12, 31), crearGrado("Ed. Media Técnica", 5), (short) 2026));
    }

    @Test
        void rechazaSextoAnioDeMediaTecnica() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.validarElegibilidad(
                LocalDate.of(2008, 12, 31), crearGrado("Educación Media Técnica", 6), (short) 2026));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private Grado crearGrado(String nombreNivel, int numeroGrado) {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre(nombreNivel);

        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) numeroGrado);
        return grado;
    }
}
