package com.tesis.service;

import com.tesis.dto.PreinscripcionDTO.PreinscripcionRequestDTO;
import com.tesis.dto.PreinscripcionDTO.PreinscripcionResponseDTO;
import com.tesis.dto.PreinscripcionDTO.EstudianteData;
import com.tesis.dto.PreinscripcionDTO.RepresentanteData;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.Representante;
import com.tesis.entity.Seccion;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PreinscripcionServiceTest {

    @Autowired
    private PreinscripcionService preinscripcionService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private RepresentanteRepository representanteRepository;

    @Autowired
    private RelacionEstudianteRepresentanteRepository relacionRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Test
    void preinscribeEstudianteYActualizaRepresentanteExistente() {
        Seccion seccion = crearSeccion(2, "A");
        Representante representante = new Representante();
        representante.setCedula("REP-PREINS-01");
        representante.setNombre("Nombre anterior");
        representante.setApellido("Apellido anterior");
        representante.setCorreo("anterior@example.com");
        representanteRepository.saveAndFlush(representante);

        PreinscripcionRequestDTO request = crearRequest(
                seccion.getId(), "EST-PREINS-01", "REP-PREINS-01");
        request.getRepresentante().setNombre("Nombre actualizado");
        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(request);

        assertEquals("preinscrito", response.getEstadoMatricula());
        assertEquals("nuevo_ingreso", matriculaRepository.findById(response.getMatriculaId())
            .orElseThrow().getTipoIngreso());
        assertEquals(1L, response.getCuposDisponibles());
        assertEquals("Nombre actualizado Apellido", response.getRepresentanteNombreCompleto());
        assertEquals(representante.getId(), response.getRepresentanteId());
        assertEquals(1, estudianteRepository.count());
        assertEquals(1, matriculaRepository.count());
        assertEquals(1, relacionRepository.findByEstudiante_IdAndActivoTrue(
                response.getEstudianteId()).size());
        assertNotNull(matriculaRepository.findAll().getFirst().getFechaSolicitud());
    }

    @Test
    void rechazaPreinscripcionCuandoLaSeccionNoTieneCupos() {
        Seccion seccion = crearSeccion(1, "B");
        Estudiante ocupante = crearEstudiante("OCUPANTE-01");

        Matricula matricula = new Matricula();
        matricula.setEstudiante(ocupante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setFechaSolicitud(LocalDate.now());
        matriculaRepository.saveAndFlush(matricula);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.preinscribir(crearRequest(
                        seccion.getId(), "EST-PREINS-02", "REP-PREINS-02")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(1, estudianteRepository.count());
        assertEquals(1, matriculaRepository.count());
    }

    @Test
    void clasificaComoRegularSiCompletoElAnioInmediatamenteAnterior() {
        Seccion seccionNueva = crearSeccion(2, "H");
        Estudiante estudiante = crearEstudiante("EST-REGULAR-ANTERIOR");
        crearMatriculaHistorica(estudiante, (short) 2025, "completada", "A");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(
                crearRequest(seccionNueva.getId(), "EST-REGULAR-ANTERIOR", "REP-REGULAR-ANTERIOR"));

        Matricula matricula = matriculaRepository.findById(response.getMatriculaId()).orElseThrow();
        assertEquals("regular", matricula.getTipoIngreso());
        assertNull(matricula.getInstitucionProcedencia());
    }

    @Test
    void estudianteExistenteSinMatriculasPreviasEsNuevoIngreso() {
        Seccion seccionNueva = crearSeccion(2, "K");
        crearEstudiante("EST-CONOCIDO");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(
                crearRequest(seccionNueva.getId(), "EST-CONOCIDO", "REP-CONOCIDO"));

        assertEquals("nuevo_ingreso", response.getTipoIngreso());
    }

    @Test
    void clasificaComoReingresoSiTieneHistorialPeroNoCompletoElAnioAnterior() {
        Seccion seccionNueva = crearSeccion(2, "I");
        Estudiante estudiante = crearEstudiante("EST-REING-24");
        crearMatriculaHistorica(estudiante, (short) 2024, "completada", "A");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(
                crearRequest(seccionNueva.getId(), "EST-REING-24", "REP-REING-24"));

        assertEquals("reingreso", matriculaRepository.findById(response.getMatriculaId())
                .orElseThrow().getTipoIngreso());
    }

    @Test
    void clasificaComoTrasladoYGuardaLaInstitucionDeProcedencia() {
        Seccion seccionNueva = crearSeccion(2, "J");
        PreinscripcionRequestDTO request = crearRequest(
                seccionNueva.getId(), "EST-TRASLADO", "REP-TRASLADO");
        request.setInstitucionProcedencia("  U.E. Simón Rodríguez  ");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(request);

        Matricula matricula = matriculaRepository.findById(response.getMatriculaId()).orElseThrow();
        assertEquals("traslado", matricula.getTipoIngreso());
        assertEquals("U.E. Simón Rodríguez", matricula.getInstitucionProcedencia());
    }

    @Test
    void rechazaSegundaMatriculaActivaDelEstudianteEnElMismoAnio() {
        Seccion primeraSeccion = crearSeccion(3, "D");
        Seccion segundaSeccion = crearSeccion(3, "E");
        PreinscripcionResponseDTO primera = preinscripcionService.preinscribir(
                crearRequest(primeraSeccion.getId(), "EST-ANUAL-01", "REP-ANUAL-01"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.preinscribir(
                        crearRequest(segundaSeccion.getId(), "EST-ANUAL-01", "REP-ANUAL-02")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(primera.getMatriculaId(), matriculaRepository.findAll().getFirst().getId());
        assertEquals(1, matriculaRepository.count());
    }

        @Test
        void rechazaUnAnioEscolarInexistente() {
        Seccion seccion = crearSeccion(2, "F");
        PreinscripcionRequestDTO request = crearRequest(
            seccion.getId(), "EST-ANIO-INEXISTENTE", "REP-ANIO-INEXISTENTE");
        request.setAnioEscolar((short) 2025);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> preinscripcionService.preinscribir(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(0, estudianteRepository.count());
        }

        @Test
        void rechazaUnAnioEscolarIncompatibleConLaSeccion() {
        Seccion seccion = crearSeccion(2, "G");
        AnioEscolar anioAnterior = new AnioEscolar();
        anioAnterior.setAnio((short) 2025);
        anioAnterior.setNombre("2025-2026");
        anioAnterior.setFechaInicio(LocalDate.of(2025, 9, 9));
        anioAnterior.setFechaFin(LocalDate.of(2026, 7, 30));
        anioAnterior.setActivo(false);
        entityManager.persist(anioAnterior);
        entityManager.flush();
        PreinscripcionRequestDTO request = crearRequest(
            seccion.getId(), "EST-ANIO-INCOMPATIBLE", "REP-ANIO-INCOMPATIBLE");
        request.setAnioEscolar((short) 2025);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> preinscripcionService.preinscribir(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

    @Test
        void reactivaMatriculaRetiradaSinModificarLosDatosDelEstudiante() {
        Seccion seccion = crearSeccion(2, "C");
        Estudiante estudiante = crearEstudiante("EST-REINGRESO");
        Matricula matriculaRetirada = new Matricula();
        matriculaRetirada.setEstudiante(estudiante);
        matriculaRetirada.setSeccion(seccion);
        matriculaRetirada.setAnioEscolar((short) 2026);
        matriculaRetirada.setFechaSolicitud(LocalDate.of(2026, 1, 1));
        matriculaRetirada.setFechaFormalizacion(LocalDate.of(2026, 1, 15));
        matriculaRetirada.setEstadoMatricula("retirada");
        matriculaRetirada = matriculaRepository.saveAndFlush(matriculaRetirada);

        PreinscripcionRequestDTO request = crearRequest(
            seccion.getId(), "EST-REINGRESO", "REP-PREINS-03");
        request.getEstudiante().setNombre("Nombre que no debe reemplazar al existente");
        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(request);

        assertEquals(matriculaRetirada.getId(), response.getMatriculaId());
        assertEquals(estudiante.getId(), response.getEstudianteId());
        assertEquals("Ocupante Prueba", response.getEstudianteNombreCompleto());
        assertEquals("preinscrito", response.getEstadoMatricula());
        assertEquals(LocalDate.now(), matriculaRepository.findById(matriculaRetirada.getId())
            .orElseThrow().getFechaSolicitud());
        assertEquals("Ocupante", estudianteRepository.findById(estudiante.getId())
            .orElseThrow().getNombre());
        assertEquals(1, estudianteRepository.count());
        assertEquals(1, matriculaRepository.count());
    }

    private Seccion crearSeccion(int capacidad, String letra) {
        return crearSeccion((short) 2026, capacidad, letra);
    }

    private Seccion crearSeccion(Short anioEscolar, int capacidad, String letra) {
        asegurarAnioEscolar(anioEscolar);
        NivelEducativo nivel = entityManager.createQuery(
                        "select n from NivelEducativo n where n.nombre = :nombre", NivelEducativo.class)
                .setParameter("nombre", "Educación Primaria")
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    NivelEducativo nuevoNivel = new NivelEducativo();
                    nuevoNivel.setNombre("Educación Primaria");
                    nuevoNivel.setOrdinal((short) 80);
                    entityManager.persist(nuevoNivel);
                    return nuevoNivel;
                });

        Grado grado = entityManager.createQuery(
                "select g from Grado g where g.nivelEducativo.id = :nivelId and g.numeroGrado = 1", Grado.class)
            .setParameter("nivelId", nivel.getId())
            .getResultStream()
            .findFirst()
            .orElseGet(() -> {
                Grado nuevoGrado = new Grado();
                nuevoGrado.setNivelEducativo(nivel);
                nuevoGrado.setNumeroGrado((short) 1);
                entityManager.persist(nuevoGrado);
                return nuevoGrado;
            });

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion(letra);
        seccion.setCapacidadMaxima((short) capacidad);
        seccion.setAnioEscolar(anioEscolar);
        entityManager.persist(seccion);
        entityManager.flush();
        return seccion;
    }

    private Matricula crearMatriculaHistorica(Estudiante estudiante,
                                               Short anioEscolar,
                                               String estado,
                                               String letra) {
        Seccion seccion = crearSeccion(anioEscolar, 10, letra);
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar(anioEscolar);
        matricula.setFechaSolicitud(LocalDate.of(anioEscolar, 9, 10));
        matricula.setEstadoMatricula(estado);
        if ("completada".equals(estado)) {
            matricula.setFechaFormalizacion(LocalDate.of(anioEscolar, 9, 15));
        }
        return matriculaRepository.saveAndFlush(matricula);
    }

    private void asegurarAnioEscolar(Short anio) {
        entityManager.createQuery("select a from AnioEscolar a where a.anio = :anio", AnioEscolar.class)
                .setParameter("anio", anio)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    AnioEscolar nuevo = new AnioEscolar();
                    nuevo.setAnio(anio);
                    nuevo.setNombre(anio + "-" + (anio + 1));
                    nuevo.setFechaInicio(LocalDate.of(anio, 9, 9));
                    nuevo.setFechaFin(LocalDate.of(anio + 1, 7, 30));
                    nuevo.setActivo(anio == 2026);
                    entityManager.persist(nuevo);
                    return nuevo;
                });

        boolean periodoExiste = !entityManager.createQuery(
                        "select p.id from PeriodoMatricula p where p.anioEscolar = :anio "
                                + "and p.tipo = :tipo", Integer.class)
                .setParameter("anio", anio)
                .setParameter("tipo", TipoPeriodoMatricula.preinscripcion)
                .getResultList().isEmpty();
        if (!periodoExiste) {
            LocalDate hoy = LocalDate.now(clock);
            PeriodoMatricula periodo = new PeriodoMatricula();
            periodo.setAnioEscolar(anio);
            periodo.setTipo(TipoPeriodoMatricula.preinscripcion);
            periodo.setFechaInicio(hoy.minusYears(1));
            periodo.setFechaFin(hoy.plusYears(1));
            periodo.setActivo(true);
            entityManager.persist(periodo);
        }
    }

    private Estudiante crearEstudiante(String cedula) {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombre("Ocupante");
        estudiante.setApellido("Prueba");
        estudiante.setFechaNacimiento(LocalDate.of(2020, 1, 1));
        estudiante.setSexo("M");
        return estudianteRepository.saveAndFlush(estudiante);
    }

    private PreinscripcionRequestDTO crearRequest(Integer seccionId,
                                                  String cedulaEstudiante,
                                                  String cedulaRepresentante) {
        PreinscripcionRequestDTO request = new PreinscripcionRequestDTO();
        request.setSeccionId(seccionId);
        request.setAnioEscolar((short) 2026);

        EstudianteData estudiante = new EstudianteData();
        estudiante.setCedula(cedulaEstudiante);
        estudiante.setNombre("Ana");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2020, 1, 1));
        estudiante.setSexo("F");
        request.setEstudiante(estudiante);

        RepresentanteData representante = new RepresentanteData();
        representante.setCedula(cedulaRepresentante);
        representante.setNombre("Nombre");
        representante.setApellido("Apellido");
        representante.setParentesco("Madre");
        representante.setCorreo("representante@example.com");
        request.setRepresentante(representante);
        return request;
    }
}