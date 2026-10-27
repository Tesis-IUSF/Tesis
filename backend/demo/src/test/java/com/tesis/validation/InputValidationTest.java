package com.tesis.validation;

import com.tesis.dto.BlogDTO;
import com.tesis.dto.CatalogoDTO;
import com.tesis.dto.EmpleadoDTO;
import com.tesis.dto.AuthDTO;
import com.tesis.dto.PreinscripcionDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void limitaCapacidadMaximaDeSeccionA60() {
        CatalogoDTO.SeccionRequestDTO solicitud = new CatalogoDTO.SeccionRequestDTO();
        solicitud.setGradoId(1);
        solicitud.setLetraSeccion("A");
        solicitud.setCapacidadMaxima((short) 61);
        solicitud.setAnioEscolar((short) 2026);

        assertTrue(tieneViolacion(solicitud, "capacidadMaxima"));
        solicitud.setCapacidadMaxima((short) 60);
        assertFalse(tieneViolacion(solicitud, "capacidadMaxima"));
    }

    @Test
    void acotaNumeroDeGradoYMinutosDeTurno() {
        CatalogoDTO.GradoRequestDTO grado = new CatalogoDTO.GradoRequestDTO();
        grado.setNivelEducativoId(1);
        grado.setNumeroGrado((short) 13);
        grado.setEdadMinima((short) 0);
        grado.setEdadMaxima((short) 12);
        assertTrue(tieneViolacion(grado, "numeroGrado"));

        CatalogoDTO.TurnoRequestDTO turno = new CatalogoDTO.TurnoRequestDTO();
        turno.setNombre("Mañana");
        turno.setHoraEntrada(java.time.LocalTime.of(7, 0));
        turno.setHoraSalida(java.time.LocalTime.of(12, 0));
        turno.setToleranciaMin((short) 181);
        assertTrue(tieneViolacion(turno, "toleranciaMin"));
        turno.setToleranciaMin((short) 0);
        turno.setMinutosSalidaAnticipadaPermitidos((short) 181);
        assertTrue(tieneViolacion(turno, "minutosSalidaAnticipadaPermitidos"));
    }

    @Test
    void limitaOrdenesDeBlogA999() {
        BlogDTO.CategoriaRequestDTO categoria =
                new BlogDTO.CategoriaRequestDTO("Noticias", null, null, "#FFFFFF",
                        (short) 1000, true);
        assertTrue(tieneViolacion(categoria, "ordinal"));

        BlogDTO.ComunicadoRequestDTO comunicado =
                new BlogDTO.ComunicadoRequestDTO("Aviso", "Contenido", null,
                        1, false, null, null, null, 1000, false);
        assertTrue(tieneViolacion(comunicado, "ordenVisualizacion"));
    }

    @Test
    void exigeFormatoTelefonicoVenezolanoYRechazaNacimientoFuturo() {
        PreinscripcionDTO.EstudianteData estudiante =
                new PreinscripcionDTO.EstudianteData();
        estudiante.setTelefono("0412ABC1234");
        assertTrue(tieneViolacion(estudiante, "telefono"));
        estudiante.setTelefono("+58 4121234567");
        assertFalse(tieneViolacion(estudiante, "telefono"));

        EmpleadoDTO.EmpleadoRequestDTO empleado = new EmpleadoDTO.EmpleadoRequestDTO();
        empleado.setFechaNacimiento(LocalDate.now().plusDays(1));
        assertTrue(tieneViolacion(empleado, "fechaNacimiento"));
    }

    @Test
    void exigeCorreoValidoEnLaSolicitudDeInicioDeSesion() {
        AuthDTO.LoginRequest solicitud = new AuthDTO.LoginRequest();
        solicitud.setEmail("no-es-un-correo");
        solicitud.setPassword("clave");

        assertTrue(tieneViolacion(solicitud, "email"));
    }

    private boolean tieneViolacion(Object objeto, String campo) {
        return validator.validate(objeto).stream()
                .anyMatch(violacion -> campo.equals(violacion.getPropertyPath().toString()));
    }
}
