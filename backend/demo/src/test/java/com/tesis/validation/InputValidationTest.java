package com.tesis.validation;

import com.tesis.dto.BlogDTO;
import com.tesis.dto.CatalogoDTO;
import com.tesis.dto.EmpleadoDTO;
import com.tesis.dto.AuthDTO;
import com.tesis.dto.PreinscripcionDTO;
import com.tesis.dto.UsuarioDTO;
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
        solicitud.setLetraSeccion("1");
        solicitud.setCapacidadMaxima((short) 61);
        solicitud.setAnioEscolar((short) 2026);

        assertTrue(tieneViolacion(solicitud, "capacidadMaxima"));
        assertTrue(tieneViolacion(solicitud, "letraSeccion"));
        solicitud.setCapacidadMaxima((short) 60);
        solicitud.setLetraSeccion("A");
        assertFalse(tieneViolacion(solicitud, "capacidadMaxima"));
        assertFalse(tieneViolacion(solicitud, "letraSeccion"));
    }

    @Test
    void acotaNumeroDeGradoYMinutosDeTurno() {
        CatalogoDTO.GradoRequestDTO grado = new CatalogoDTO.GradoRequestDTO();
        grado.setNivelEducativoId(1);
        grado.setNumeroGrado((short) 13);
        grado.setEdadMinima((short) 0);
        grado.setEdadMaxima((short) 12);
        assertTrue(tieneViolacion(grado, "numeroGrado"));
        assertTrue(tieneViolacion(grado, "edadMinima"));
        grado.setEdadMinima((short) 1);
        assertFalse(tieneViolacion(grado, "edadMinima"));
        grado.setEdadMinima((short) 21);
        assertTrue(tieneViolacion(grado, "edadMinima"));
        grado.setEdadMinima((short) 1);
        grado.setEdadMaxima((short) 0);
        assertTrue(tieneViolacion(grado, "edadMaxima"));
        grado.setEdadMaxima((short) 21);
        assertTrue(tieneViolacion(grado, "edadMaxima"));
        grado.setEdadMaxima((short) 20);
        assertFalse(tieneViolacion(grado, "edadMaxima"));
        grado.setEdadMaxima(null);
        assertTrue(tieneViolacion(grado, "edadMaxima"));

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

    @Test
    void validaCamposYLimitesDeUsuario() {
        UsuarioDTO.UsuarioRequestDTO solicitud = new UsuarioDTO.UsuarioRequestDTO();
        solicitud.setNombreUsuario("x".repeat(101));
        solicitud.setEmail("incorrecto");
        solicitud.setPassword("clave");
        solicitud.setRolId(0);

        assertTrue(tieneViolacion(solicitud, "nombreUsuario"));
        assertTrue(tieneViolacion(solicitud, "email"));
        assertTrue(tieneViolacion(solicitud, "rolId"));

        solicitud.setNombreUsuario("usuario");
        solicitud.setEmail("usuario@ejemplo.com");
        solicitud.setRolId(1);
        assertFalse(tieneViolacion(solicitud, "nombreUsuario"));
        assertFalse(tieneViolacion(solicitud, "email"));
        assertFalse(tieneViolacion(solicitud, "rolId"));
    }

    private boolean tieneViolacion(Object objeto, String campo) {
        return validator.validate(objeto).stream()
                .anyMatch(violacion -> campo.equals(violacion.getPropertyPath().toString()));
    }
}
