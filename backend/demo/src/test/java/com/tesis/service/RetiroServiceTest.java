package com.tesis.service;

import com.tesis.dto.RetiroDTO.HistorialRetirosResponseDTO;
import com.tesis.dto.RetiroDTO.RetiroRequestDTO;
import com.tesis.dto.RetiroDTO.RetiroResponseDTO;
import com.tesis.entity.ChecklistMatricula;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RelacionEstudianteRepresentante;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Representante;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Usuario;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import com.tesis.repository.RetiroMatriculaRepository;
import com.tesis.repository.SeccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
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
    private RetiroMatriculaRepository retiroRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private RepresentanteRepository representanteRepository;

    @Autowired
    private RelacionEstudianteRepresentanteRepository relacionRepository;

    @Autowired
    private EntityManager entityManager;

        @Autowired
        private Clock clock;

    @Test
    void registraRetiroDeSolicitanteExternoYLiberaElCupo() {
        crearProcesador();
        Matricula matricula = crearMatricula("completada", "RETIRO-EXT-01", "A");
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Cambio de domicilio", LocalDate.now(clock), null,
                "Carla", "Soto", "CED-EXT-01");

        RetiroResponseDTO response = retiroService.retirar(matricula.getId(), request, "procesador@test.local");

        assertEquals("retirada", response.getEstadoMatricula());
        assertEquals("retiro_formal", response.getOperacion());
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
    void rechazaMotivoDeRetiroConMasDe500Caracteres() {
        RetiroRequestDTO request = new RetiroRequestDTO(
                "x".repeat(501), LocalDate.now(clock), null, "Persona", "Externa", "CED-04");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> retiroService.retirar(999, request, "procesador@test.local"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
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
        relacion.setFechaVinculacion(LocalDate.now(clock));
        relacion.setAutorizadoRetirar(true);
        relacionRepository.saveAndFlush(relacion);

        RetiroRequestDTO request = new RetiroRequestDTO(
                "Solicitud de la familia", LocalDate.now(clock), representante.getId(),
                null, null, null);
        RetiroResponseDTO response = retiroService.retirar(matricula.getId(), request, "procesador@test.local");

        assertEquals(representante.getId(), response.getRepresentanteId());
        assertEquals("Representante", response.getSolicitanteNombre());
        assertEquals("REP-RET-01", response.getSolicitanteCedula());
    }

    @Test
    void rechazaRetirarFormalmenteUnaMatriculaEnProceso() {
        crearProcesador();
        Matricula matricula = crearMatricula("en_proceso", "RETIRO-PEND-01", "C");
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Solicitud", LocalDate.now(clock), null, "Persona", "Externa", "CED-02");

        org.springframework.web.server.ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> retiroService.retirar(
                                matricula.getId(), request, "procesador@test.local"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("en_proceso", matriculaRepository.findById(matricula.getId())
                .orElseThrow().getEstadoMatricula());
        assertEquals(0, retiroService.obtenerHistorial(matricula.getId()).getRetiros().size());
    }

    @Test
    void registraRetiroFormalDeContinuidadAunqueEstePreinscrita() {
        crearProcesador();
        Matricula origen = crearMatricula("en_proceso", "RET-CONT-01", "G");
        Matricula continuidad = crearMatricula("preinscrito", "RET-CONT-02", "H");
        continuidad.setContinuidadDeMatricula(origen);
        continuidad.setEstudiante(origen.getEstudiante());
        continuidad.setAnioEscolar((short) 2027);
        continuidad = matriculaRepository.saveAndFlush(continuidad);
        RetiroRequestDTO request = new RetiroRequestDTO(
                "No continuará", LocalDate.now(clock), null, "Persona", "Externa", "CED-CONT-01");

        RetiroResponseDTO response = retiroService.retirar(
                continuidad.getId(), request, "procesador@test.local");

        assertEquals("retiro_formal", response.getOperacion());
        assertEquals("retirada", response.getEstadoMatricula());
        assertEquals(true, matriculaRepository.existsById(continuidad.getId()));
        assertEquals("en_proceso", matriculaRepository.findById(origen.getId())
                .orElseThrow().getEstadoMatricula());
        assertEquals(1, retiroService.obtenerHistorial(continuidad.getId()).getRetiros().size());
    }

    @Test
    void eliminaSoloLaMatriculaPreinscritaYConservaElEstudiante() {
        crearProcesador();
        Matricula matricula = crearMatricula("preinscrito", "PREINS-ELIMINAR-01", "D");
        Integer estudianteId = matricula.getEstudiante().getId();
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Carga equivocada", LocalDate.now(clock), null, "Persona", "Externa", "CED-03");

        RetiroResponseDTO response = retiroService.retirar(
                matricula.getId(), request, "procesador@test.local");

        assertEquals("preinscripcion_eliminada", response.getOperacion());
        assertEquals("preinscrito", response.getEstadoMatricula());
        assertEquals(null, response.getRetiroId());
        assertEquals(false, matriculaRepository.existsById(matricula.getId()));
        assertEquals(true, entityManager.find(Estudiante.class, estudianteId) != null);
        assertEquals(0, retiroRepository.count());
    }

    @Test
    void rechazaEliminarPreinscripcionConChecklistAsociado() {
        crearProcesador();
        Matricula matricula = crearMatricula("preinscrito", "PREINS-CHECK-01", "E");
        RequisitoMatricula requisito = new RequisitoMatricula();
        requisito.setNombre("Requisito");
        requisito.setActivo(true);
        entityManager.persist(requisito);
        ChecklistMatricula checklist = new ChecklistMatricula();
        checklist.setMatricula(matricula);
        checklist.setRequisito(requisito);
        checklist.setCumplido(false);
        entityManager.persist(checklist);
        entityManager.flush();
        RetiroRequestDTO request = new RetiroRequestDTO(
                "Cancelación", LocalDate.now(clock), null, "Persona", "Externa", "CED-04");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> retiroService.retirar(matricula.getId(), request, "procesador@test.local"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(true, matriculaRepository.existsById(matricula.getId()));
        assertEquals(0, retiroService.obtenerHistorial(matricula.getId()).getRetiros().size());
    }

    @Test
    void rechazaEliminarPreinscripcionConRetiroPrevioAsociado() {
        crearProcesador();
        Matricula matricula = crearMatricula("completada", "PREINS-RETIRO-PREVIO", "F");
        RetiroRequestDTO retiroFormal = new RetiroRequestDTO(
                "Retiro anterior", LocalDate.now(clock), null, "Persona", "Externa", "CED-05");
        retiroService.retirar(matricula.getId(), retiroFormal, "procesador@test.local");
        matricula.setEstadoMatricula("preinscrito");
        matricula.setFechaFormalizacion(null);
        matriculaRepository.saveAndFlush(matricula);

        RetiroRequestDTO cancelacion = new RetiroRequestDTO(
                "Cancelación", LocalDate.now(clock), null, "Persona", "Externa", "CED-06");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> retiroService.retirar(
                        matricula.getId(), cancelacion, "procesador@test.local"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(true, matriculaRepository.existsById(matricula.getId()));
        assertEquals(1, retiroRepository.findByMatricula_IdOrderByFechaRetiroDesc(
                matricula.getId()).size());
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
        grado.setEdadMinima((short) 6);
        grado.setEdadMaxima((short) 7);
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
        if ("completada".equals(estado)) {
            matricula.setFechaFormalizacion(LocalDate.of(2026, 1, 15));
        }
        matricula.setEstadoMatricula(estado);
        entityManager.persist(matricula);
        entityManager.flush();
        return matricula;
    }
}