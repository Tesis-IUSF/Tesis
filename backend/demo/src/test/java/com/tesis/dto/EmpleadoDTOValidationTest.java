package com.tesis.dto;

import com.tesis.dto.EmpleadoDTO.CrearEmpleado;
import com.tesis.dto.EmpleadoDTO.EmpleadoRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpleadoDTOValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void exigeTurnoYFechaDeIngresoAlCrearEmpleado() {
        EmpleadoRequestDTO request = solicitudValida();
        request.setTurnoId(null);
        request.setFechaIngreso(null);

        var errores = validator.validate(request, CrearEmpleado.class);

        assertTrue(errores.stream().anyMatch(error -> error.getPropertyPath().toString().equals("turnoId")));
        assertTrue(errores.stream().anyMatch(error -> error.getPropertyPath().toString().equals("fechaIngreso")));
    }

    @Test
    void aceptaSolicitudDeCreacionConTurnoYFechaDeIngreso() {
        assertFalse(validator.validate(solicitudValida(), CrearEmpleado.class).iterator().hasNext());
    }

    @Test
    void exigeCargoAlCrearEmpleado() {
        EmpleadoRequestDTO request = solicitudValida();
        request.setCargoId(null);

        var errores = validator.validate(request, CrearEmpleado.class);

        assertTrue(errores.stream().anyMatch(error -> error.getPropertyPath().toString().equals("cargoId")));
    }

    @Test
    void permiteDepartamentoOpcionalAlCrearEmpleado() {
        EmpleadoRequestDTO request = solicitudValida();
        request.setDepartamentoId(null);

        var errores = validator.validate(request, CrearEmpleado.class);

        assertTrue(errores.stream().noneMatch(error -> error.getPropertyPath().toString().equals("departamentoId")));
    }

    private EmpleadoRequestDTO solicitudValida() {
        EmpleadoRequestDTO request = new EmpleadoRequestDTO();
        request.setNombre("Ana");
        request.setApellido("Pérez");
        request.setCedula("12345678");
        request.setCargoId(10);
        request.setFechaIngreso(LocalDate.of(2020, 1, 15));
        request.setTurnoId(4);
        return request;
    }
}
