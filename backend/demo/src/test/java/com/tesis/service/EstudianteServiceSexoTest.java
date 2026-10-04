package com.tesis.service;

import com.tesis.entity.Estudiante;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceSexoTest {

    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private EstudianteService service;

    @BeforeEach
    void setUp() {
        service = new EstudianteService(estudianteRepository, usuarioRepository);
    }

    @Test
    void rechazaSexoFueraDeLosValoresPermitidosAntesDeConsultarRepositorios() {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula("EST-SEX-01");
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Prueba");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("H");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(estudiante));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(estudianteRepository, usuarioRepository);
    }

    @Test
    void beanValidationSoloAceptaLosCodigosDeSexoDelBackend() {
        Estudiante estudiante = new Estudiante();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            estudiante.setSexo("H");
            assertFalse(validator.validateProperty(estudiante, "sexo").isEmpty());

            estudiante.setSexo("M");
            assertTrue(validator.validateProperty(estudiante, "sexo").isEmpty());
        }
    }
}