package com.tesis.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CedulaEstudiantilTest {

    @Test
    void generaCedulaEstudiantilConElFormatoDefinido() {
        String cedula = CedulaEstudiantil.generar(
                "v", 1, LocalDate.of(2018, 3, 15), "V-18.456.789");

        assertEquals("CE-V11818456789", cedula);
        assertTrue(CedulaEstudiantil.esValida(cedula));
    }

    @Test
    void completaConCeroLaCedulaDeSieteDigitosDelRepresentante() {
        String cedula = CedulaEstudiantil.generar(
                "E", 3, LocalDate.of(2009, 12, 1), "V-7.654.321");

        assertEquals("CE-E30907654321", cedula);
    }

    @Test
    void rechazaOrdenDeNacimientoQueNoCabeEnUnDigito() {
        assertThrows(IllegalArgumentException.class, () -> CedulaEstudiantil.generar(
                "V", 10, LocalDate.of(2018, 1, 1), "V-18456789"));
    }

    @Test
    void normalizaYValidaElFormatoEstudiantil() {
        assertEquals("CE-V11818456789", CedulaEstudiantil.normalizar(" ce-v11818456789 "));
        assertFalse(CedulaEstudiantil.esValida("CE-V1181845678"));
        assertFalse(CedulaEstudiantil.esValida("CE-V01818456789"));
    }

    @Test
    void normalizaCedulaDeRepresentanteConSeparadoresDePresentacion() {
        String cedula = CedulaVenezolana.normalizarFormato(" v-18.456.789 ");

        assertEquals("V-18456789", cedula);
        assertTrue(CedulaVenezolana.esValida(cedula));
        assertFalse(CedulaVenezolana.esValida(
                CedulaVenezolana.normalizarFormato("E-79.999.999")));
    }
}
