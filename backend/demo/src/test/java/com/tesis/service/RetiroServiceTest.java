package com.tesis.service;

import com.tesis.dto.RetiroDTO.HistorialRetirosResponseDTO;
import com.tesis.dto.RetiroDTO.RetiroRequestDTO;
import com.tesis.dto.RetiroDTO.RetiroResponseDTO;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RelacionEstudianteRepresentante;
import com.tesis.entity.Representante;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Usuario;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.SeccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RetiroServiceTest {

    @Autowired
    private RetiroService retiroService;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private RepresentanteRepository representanteRepository;

    @Autowired
    private RelacionEstudianteRepresentanteRepository relacionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registraRetiroDeSolicitanteExternoYLiberaElCupo() {
        crearProcesador();
        Matricula matricula = crearMatricula("completada", "RETIRO-EXT-01", "A");
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Cambio de domicilio", LocalDate.now(), null,
                "Carla", "Soto", "CED-EXT-01");

        RetiroResponseDTO response = retiroService.retirar(matricula.getId(), request, "procesador@test.local");

        assertEquals("retirada", response.getEstadoMatricula());
        assertEquals("Carla", response.getSolicitanteNombre());
        assertEquals("CED-EXT-01", response.getSolicitanteCedula());
        assertNotNull(response.getProcesadoPorId());
        assertEquals("retirada", matriculaRepository.findById(matricula.getId())
                .orElseThrow().getEstadoMatricula());
        assertEquals(2L, seccionRepository.calcularCuposDisponibles(
                matricula.getSeccion().getId()).orElseThrow());

        HistorialRetirosResponseDTO historial = retiroService.obtenerHistorial(matricula.getId());
        assertEquals(1, historial.getRetiros().size());
        assertEquals("Cambio de domicilio", historial.getRetiros().getFirst().getMotivo());
    }

    @Test
    void permiteRetiroPorRepresentanteVinculadoYAutorizado() {
                crearProcesador();
        Matricula matricula = crearMatricula("completada", "RETIRO-REP-01", "B");
        Representante representante = new Representante();
        representante.setCedula("REP-RET-01");
        representante.setNombre("Representante");
        representante.setApellido("Autorizado");
        representante.setCorreo("rep@example.com");
        representante = representanteRepository.saveAndFlush(representante);

        RelacionEstudianteRepresentante relacion = new RelacionEstudianteRepresentante();
        relacion.setEstudiante(matricula.getEstudiante());
        relacion.setRepresentante(representante);
        relacion.setFechaVinculacion(LocalDate.now());
        relacion.setAutorizadoRetirar(true);
        relacionRepository.saveAndFlush(relacion);

        RetiroRequestDTO request = new RetiroRequestDTO(
                "Solicitud de la familia", LocalDate.now(), representante.getId(),
                null, null, null);
        RetiroResponseDTO response = retiroService.retirar(matricula.getId(), request, "procesador@test.local");

        assertEquals(representante.getId(), response.getRepresentanteId());
        assertEquals("Representante", response.getSolicitanteNombre());
        assertEquals("REP-RET-01", response.getSolicitanteCedula());
    }

    @Test
    void rechazaRetirarMatriculaNoCompletada() {
        crearProcesador();
        Matricula matricula = crearMatricula("en_proceso", "RETIRO-PEND-01", "C");
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Solicitud", LocalDate.now(), null, "Persona", "Externa", "CED-02");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> retiroService.retirar(matricula.getId(), request, "procesador@test.local"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(0, retiroService.obtenerHistorial(matricula.getId()).getRetiros().size());
    }

        private void crearProcesador() {
                Roles rol = new Roles();
                rol.setNombreRol("Administrador");
                entityManager.persist(rol);

                Usuario usuario = new Usuario();
                usuario.setNombreUsuario("procesador");
                usuario.setEmail("procesador@test.local");
                usuario.setPasswordHash("hash");
                usuario.setRol(rol);
                entityManager.persist(usuario);
                entityManager.flush();
        }

    private Matricula crearMatricula(String estado, String cedulaEstudiante, String letra) {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Nivel retiro " + letra);
        nivel.setOrdinal((short) (120 + letra.charAt(0)));
        entityManager.persist(nivel);

        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        entityManager.persist(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion(letra);
        seccion.setCapacidadMaxima((short) 2);
        seccion.setAnioEscolar((short) 2026);
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedulaEstudiante);
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Retirado");
        estudiante.setFechaNacimiento(LocalDate.of(2017, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setFechaSolicitud(LocalDate.of(2026, 1, 1));
        matricula.setFechaFormalizacion(LocalDate.of(2026, 1, 15));
        matricula.setEstadoMatricula(estado);
        entityManager.persist(matricula);
        entityManager.flush();
        return matricula;
    }
}