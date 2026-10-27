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
import com.tesis.entity.ChecklistMatricula;
import com.tesis.entity.Representante;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Seccion;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.EstudianteRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import com.tesis.repository.RepresentanteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

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
    private ChecklistMatriculaRepository checklistRepository;

    @Autowired
    private RequisitoMatriculaRepository requisitoRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private RepresentanteService representanteService;

    @Test
    void normalizaCedulaDelRepresentanteEnElCrudDirecto() {
        Representante representante = new Representante();
        representante.setCedula(" v-18.456.789 ");
        representante.setNombre("Nombre");
        representante.setApellido("Apellido");
        representante.setCorreo("representante@example.com");
        representante.setTelefono(" 0412-1234567 ");

        Representante creado = representanteService.crear(representante);

        assertEquals("V-18456789", creado.getCedula());
        assertEquals("0412-1234567", creado.getTelefono());
    }

    @Test
    void exigeTelefonoPrincipalEnElCrudDeRepresentante() {
        Representante representante = new Representante();
        representante.setCedula("V-18456789");
        representante.setNombre("Nombre");
        representante.setApellido("Apellido");
        representante.setCorreo("representante@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> representanteService.crear(representante));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void buscaRepresentantesActivosPorNombreOCedulaConPaginacion() {
        Representante activo = nuevoRepresentante("V-18456789", "Ana", "Pérez");
        Representante inactivo = nuevoRepresentante("V-18456790", "Ana", "Gómez");
        inactivo.setActivo(false);
        representanteRepository.saveAndFlush(inactivo);

        var porNombre = representanteService.buscarActivos("Ana", PageRequest.of(0, 1));
        var porCedula = representanteService.buscarActivos("V-18456789", PageRequest.of(0, 10));

        assertEquals(1, porNombre.getTotalElements());
        assertEquals(activo.getId(), porNombre.getContent().getFirst().getId());
        assertEquals(1, porCedula.getTotalElements());
        assertEquals(activo.getId(), porCedula.getContent().getFirst().getId());
    }

    private Representante nuevoRepresentante(String cedula, String nombre, String apellido) {
        Representante representante = new Representante();
        representante.setCedula(cedula);
        representante.setNombre(nombre);
        representante.setApellido(apellido);
        representante.setCorreo("representante@example.com");
        representante.setTelefono("04121234567");
        return representanteRepository.saveAndFlush(representante);
    }

    @Test
    void conservaCedulaHistoricaDelRepresentanteMientrasNoSeCambie() {
        Representante historico = new Representante();
        historico.setCedula("7654321");
        historico.setNombre("Nombre");
        historico.setApellido("Anterior");
        historico.setCorreo("representante@example.com");
        representanteRepository.saveAndFlush(historico);

        Representante actualizacion = new Representante();
        actualizacion.setCedula("7654321");
        actualizacion.setNombre("Nombre");
        actualizacion.setApellido("Actualizado");
        actualizacion.setCorreo("representante@example.com");
        actualizacion.setTelefono("04121234567");

        Representante actualizado =
                representanteService.actualizar(historico.getId(), actualizacion);

        assertEquals("7654321", actualizado.getCedula());
        assertEquals("Actualizado", actualizado.getApellido());
    }

    @Test
    void normalizaYValidaCedulaEstudiantilEnElCrudDirecto() {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(" v-18.456.789 ");
        estudiante.setNombre("Ana");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("F");

        Estudiante creado = estudianteService.crear(estudiante);

        assertEquals("V-18456789", creado.getCedula());
    }

    @Test
    void noPermiteCrearManualmenteUnaCedulaEscolar() {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula("CE-V11818456789");
        estudiante.setNombre("Ana");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("F");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> estudianteService.crear(estudiante));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rechazaCedulaEstudiantilQueNoRespetaElFormato() {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula("12345678");
        estudiante.setNombre("Ana");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("F");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> estudianteService.crear(estudiante));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void preinscribeEstudianteYActualizaRepresentanteExistente() {
        Seccion seccion = crearSeccion(2, "A");
        Representante representante = new Representante();
        representante.setCedula(cedulaRepresentanteDeClave("REP-PREINS-01"));
        representante.setNombre("Nombre anterior");
        representante.setApellido("Apellido anterior");
        representante.setCorreo("anterior@example.com");
        representanteRepository.saveAndFlush(representante);

        PreinscripcionRequestDTO request = crearRequest(
                seccion.getId(), "EST-PREINS-01", "REP-PREINS-01");
        request.getRepresentante().setId(representante.getId());
        request.getRepresentante().setNombre("Nombre actualizado");
        request.getRepresentante().setFotoUrl("/api/fotos/representante.png");
        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(request);

        assertEquals("preinscrito", response.getEstadoMatricula());
        assertEquals("nuevo_ingreso", matriculaRepository.findById(response.getMatriculaId())
            .orElseThrow().getTipoIngreso());
        assertEquals(1L, response.getCuposDisponibles());
        assertEquals("Nombre actualizado Apellido", response.getRepresentanteNombreCompleto());
        assertEquals("04121234567", representanteRepository
                .findById(representante.getId()).orElseThrow().getTelefono());
        assertEquals("/api/fotos/representante.png", representanteRepository
                .findByCedula(cedulaRepresentanteDeClave("REP-PREINS-01"))
                .orElseThrow().getFotoUrl());
        assertEquals(representante.getId(), response.getRepresentanteId());
        assertEquals(1, estudianteRepository.count());
        assertEquals(1, matriculaRepository.count());
        assertEquals(1, relacionRepository.findByEstudiante_IdAndActivoTrue(
                response.getEstudianteId()).size());
        assertNotNull(matriculaRepository.findAll().getFirst().getFechaSolicitud());
    }

    @Test
    void validaEdadConLaFechaGuardadaCuandoLaCedulaYaExiste() {
        Seccion seccion = crearSeccion(2, "E");
        Estudiante existente = crearEstudiante("EST-EDAD-GUARDADA");
        existente.setFechaNacimiento(LocalDate.of(2017, 1, 1));
        estudianteRepository.saveAndFlush(existente);
        PreinscripcionRequestDTO request = crearRequest(
                seccion.getId(), "EST-EDAD-GUARDADA", "REP-EDAD-GUARDADA");
        request.getEstudiante().setFechaNacimiento(LocalDate.of(2020, 1, 1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> preinscripcionService.validarEdadNuevaPreinscripcion(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void generaLaCedulaEstudiantilDesdeLosDatosDelEstudianteYRepresentante() {
        Seccion seccion = crearSeccion(2, "X");
        PreinscripcionRequestDTO request = crearRequest(
                seccion.getId(), "EST-CI-GENERADA", "REP-CI-GENERADA");
        request.getEstudiante().setCedula(null);
        request.getEstudiante().setTieneCedulaIdentidad(false);
        request.getEstudiante().setNacionalidad("v");
        request.getEstudiante().setOrdenNacimiento(1);
        request.getEstudiante().setFechaNacimiento(LocalDate.of(2019, 1, 1));
        request.getRepresentante().setCedula("V-18.456.789");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(request);

        assertEquals("CE-V11918456789", response.getEstudianteCedula());
        assertEquals("CE-V11918456789", estudianteRepository.findById(response.getEstudianteId())
                .orElseThrow().getCedula());
        assertEquals("V-18456789", representanteRepository
                .findById(response.getRepresentanteId()).orElseThrow().getCedula());
    }

    @Test
    void rechazaOrdenDeNacimientoFueraDelFormatoDeUnDigito() {
        Seccion seccion = crearSeccion(2, "Y");
        PreinscripcionRequestDTO request = crearRequest(
                seccion.getId(), "EST-CI-INVALIDA", "REP-CI-INVALIDA");
        request.getEstudiante().setCedula(null);
        request.getEstudiante().setTieneCedulaIdentidad(false);
        request.getEstudiante().setOrdenNacimiento(10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.preinscribir(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(0, estudianteRepository.count());
    }

    @Test
    void actualizaCedulaEscolarAIdentidadRealAlEditarLaPreinscripcion() {
        Seccion seccion = crearSeccion(2, "W");
        PreinscripcionRequestDTO solicitud = crearRequest(
                seccion.getId(), "EST-CI-A-IDENTIDAD", "REP-CI-A-IDENTIDAD");
        solicitud.getEstudiante().setTieneCedulaIdentidad(false);
        solicitud.getEstudiante().setCedula(null);
        solicitud.getEstudiante().setNacionalidad("V");
        solicitud.getEstudiante().setOrdenNacimiento(1);
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(solicitud);
        PreinscripcionRequestDTO edicion = preinscripcionService
                .obtenerDatosPreinscripcion(creada.getMatriculaId());
        edicion.getEstudiante().setTieneCedulaIdentidad(true);
        edicion.getEstudiante().setCedula("E-81234567");

        PreinscripcionResponseDTO actualizada = preinscripcionService
                .actualizarPreinscripcion(creada.getMatriculaId(), edicion);

        assertEquals("E-81234567", actualizada.getEstudianteCedula());
        assertEquals("E-81234567", estudianteRepository.findById(creada.getEstudianteId())
                .orElseThrow().getCedula());
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
    void clasificaComoRegularSiTieneMatriculaCompletadaElAnioInmediatamenteAnterior() {
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
    void clasificaComoRegularSiTieneMatriculaEnProcesoElAnioInmediatamenteAnterior() {
        Seccion seccionNueva = crearSeccion(2, "W");
        Estudiante estudiante = crearEstudiante("EST-REGULAR-PROCESO");
        crearMatriculaHistorica(estudiante, (short) 2025, "en_proceso", "X");

        PreinscripcionResponseDTO response = preinscripcionService.preinscribir(
                crearRequest(seccionNueva.getId(), "EST-REGULAR-PROCESO", "REP-REGULAR-PROCESO"));

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
    void actualizaSoloLaFotoDeUnEstudianteExistenteCuandoSeEnviaUnaNueva() {
        Seccion seccionNueva = crearSeccion(2, "L");
        Estudiante estudiante = crearEstudiante("EST-FOTO-EXISTENTE");
        estudiante.setFotoCarnetUrl("/api/fotos/foto-anterior.jpg");
        estudianteRepository.saveAndFlush(estudiante);

        PreinscripcionRequestDTO request = crearRequest(
                seccionNueva.getId(), "EST-FOTO-EXISTENTE", "REP-FOTO-EXISTENTE");
        request.getEstudiante().setNombre("Nombre que no se debe actualizar");
        request.getEstudiante().setFotoCarnetUrl("/api/fotos/foto-nueva.png");

        preinscripcionService.preinscribir(request);

        Estudiante actualizado = estudianteRepository.findById(estudiante.getId()).orElseThrow();
        assertEquals("Ocupante", actualizado.getNombre());
        assertEquals("/api/fotos/foto-nueva.png", actualizado.getFotoCarnetUrl());
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
        PreinscripcionRequestDTO segundaSolicitud = crearRequest(
                segundaSeccion.getId(), "EST-ANUAL-01", "REP-ANUAL-02");
        segundaSolicitud.getEstudiante().setCedula(primera.getEstudianteCedula());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.preinscribir(segundaSolicitud));

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
        assertEquals(LocalDate.now(clock), matriculaRepository.findById(matriculaRetirada.getId())
            .orElseThrow().getFechaSolicitud());
        assertEquals("Ocupante", estudianteRepository.findById(estudiante.getId())
            .orElseThrow().getNombre());
        assertEquals(1, estudianteRepository.count());
        assertEquals(1, matriculaRepository.count());
    }

    @Test
    void obtieneYActualizaLosDatosDeUnaMatriculaEnProcesoSinCambiarSuEstado() {
        Seccion seccionOriginal = crearSeccion(2, "M");
        Seccion seccionNueva = crearSeccion(2, "N");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccionOriginal.getId(), "EST-EDITAR-01", "REP-EDITAR-01"));
        Matricula matricula = matriculaRepository.findById(creada.getMatriculaId()).orElseThrow();
        matricula.setEstadoMatricula("en_proceso");
        matriculaRepository.saveAndFlush(matricula);

        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(matricula.getId());
        datos.getEstudiante().setNombre("Nombre corregido");
        datos.getRepresentante().setNombre("Representante corregido");
        datos.setSeccionId(seccionNueva.getId());
        datos.setInstitucionProcedencia("Escuela anterior");

        var respuesta = preinscripcionService.actualizarPreinscripcion(matricula.getId(), datos);

        assertEquals("en_proceso", respuesta.getEstadoMatricula());
        assertEquals(seccionNueva.getId(), respuesta.getSeccionId());
        assertEquals("traslado", respuesta.getTipoIngreso());
        assertEquals("Nombre corregido", estudianteRepository.findById(creada.getEstudianteId())
                .orElseThrow().getNombre());
        assertEquals("Representante corregido", representanteRepository
                .findByCedula(cedulaRepresentanteDeClave("REP-EDITAR-01"))
                .orElseThrow().getNombre());
        assertEquals("en_proceso", matriculaRepository.findById(matricula.getId())
                .orElseThrow().getEstadoMatricula());
    }

    @Test
    void actualizarPreinscripcionSinCambiosDevuelveElMismoFormulario() {
        Seccion seccion = crearSeccion(2, "O");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-02", "REP-EDITAR-02"));

        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(creada.getMatriculaId());

        assertEquals(creada.getEstudianteCedula(), datos.getEstudiante().getCedula());
        assertEquals(cedulaRepresentanteDeClave("REP-EDITAR-02"),
                datos.getRepresentante().getCedula());
        assertEquals(seccion.getId(), datos.getSeccionId());
        assertEquals((short) 2026, datos.getAnioEscolar());
        assertEquals(true, datos.getRelacionPrincipal());
        assertEquals(true, datos.getAutorizadoRetirar());
        assertEquals(true, datos.getRecibeComunicados());
    }

    @Test
    void permiteEditarFichaCompletadaYConservaDatosFormalizados() {
        Seccion seccion = crearSeccion(2, "P");
        Seccion nuevaSeccion = crearSeccion(2, "Q");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-03", "REP-EDITAR-03"));
        Matricula matricula = matriculaRepository.findById(creada.getMatriculaId()).orElseThrow();
        matricula.setEstadoMatricula("completada");
        matricula.setTipoIngreso("regular");
        matricula.setInstitucionProcedencia("Institución histórica");
        matricula.setFechaFormalizacion(LocalDate.of(2026, 1, 15));
        matriculaRepository.saveAndFlush(matricula);

        RequisitoMatricula requisito = crearRequisito(
                "REQUISITO-FORMALIZADO-EDITAR", seccion.getGrado().getNivelEducativo(), null);
        ChecklistMatricula checklist = crearChecklist(matricula, requisito, true);
        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(matricula.getId());
        datos.getEstudiante().setNombre("Nombre corregido");
        datos.getRepresentante().setNombre("Representante corregido");
        datos.setInstitucionProcedencia("Intento de cambio histórico");
        datos.setSeccionId(nuevaSeccion.getId());

        var respuesta = preinscripcionService.actualizarPreinscripcion(matricula.getId(), datos);

        entityManager.flush();
        entityManager.clear();
        Matricula actualizada = matriculaRepository.findById(matricula.getId()).orElseThrow();
        assertEquals("completada", respuesta.getEstadoMatricula());
        assertEquals(nuevaSeccion.getId(), respuesta.getSeccionId());
        assertEquals("completada", actualizada.getEstadoMatricula());
        assertEquals(nuevaSeccion.getId(), actualizada.getSeccion().getId());
        assertEquals("regular", actualizada.getTipoIngreso());
        assertEquals("Institución histórica", actualizada.getInstitucionProcedencia());
        assertEquals(LocalDate.of(2026, 1, 15), actualizada.getFechaFormalizacion());
        assertEquals("Nombre corregido", estudianteRepository.findById(creada.getEstudianteId())
                .orElseThrow().getNombre());
        assertEquals("Representante corregido", representanteRepository
                .findByCedula(cedulaRepresentanteDeClave("REP-EDITAR-03"))
                .orElseThrow().getNombre());
        assertEquals(checklist.getId(), checklistRepository.findByMatricula_Id(matricula.getId())
                .getFirst().getId());
        assertEquals(requisito.getId(), checklistRepository.findByMatricula_Id(matricula.getId())
                .getFirst().getRequisito().getId());
        assertEquals(true, checklistRepository.findByMatricula_Id(matricula.getId())
                .getFirst().getCumplido());
    }

    @Test
    void rechazaActualizarPreinscripcionSiLaMatriculaFueRetirada() {
        Seccion seccion = crearSeccion(2, "S");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-RETIRADA", "REP-EDITAR-RETIRADA"));
        Matricula matricula = matriculaRepository.findById(creada.getMatriculaId()).orElseThrow();
        matricula.setEstadoMatricula("retirada");
        matriculaRepository.saveAndFlush(matricula);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.actualizarPreinscripcion(
                        matricula.getId(),
                        preinscripcionService.obtenerDatosPreinscripcion(matricula.getId())));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void rechazaActualizarCedulaDeEstudianteSiPerteneceAOtro() {
        Seccion seccion = crearSeccion(2, "Q");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-04", "REP-EDITAR-04"));
        crearEstudiante("EST-EDITAR-05");
        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(creada.getMatriculaId());
        datos.getEstudiante().setCedula(cedulaEstudianteDeClave("EST-EDITAR-05"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.actualizarPreinscripcion(
                        creada.getMatriculaId(), datos));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(creada.getEstudianteCedula(),
                estudianteRepository.findById(creada.getEstudianteId())
                .orElseThrow().getCedula());
    }

    @Test
    void rechazaCambiarASeccionSinCupo() {
        Seccion seccionOriginal = crearSeccion(2, "T");
        Seccion seccionLlena = crearSeccion(1, "U");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccionOriginal.getId(), "EST-EDITAR-CUPO", "REP-EDITAR-CUPO"));
        Estudiante ocupante = crearEstudiante("EST-CUPO-OCP-01");
        crearMatriculaEnSeccion(ocupante, seccionLlena, "preinscrito");
        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(creada.getMatriculaId());
        datos.setSeccionId(seccionLlena.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.actualizarPreinscripcion(
                        creada.getMatriculaId(), datos));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(seccionOriginal.getId(), matriculaRepository
                .findById(creada.getMatriculaId()).orElseThrow().getSeccion().getId());
    }

    @Test
    void rechazaCambioDeFechaDeNacimientoQueNoEsElegibleParaElGrado() {
        Seccion seccion = crearSeccion(2, "V");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-EDAD", "REP-EDITAR-EDAD"));
        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(creada.getMatriculaId());
        datos.getEstudiante().setFechaNacimiento(LocalDate.of(2017, 1, 1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preinscripcionService.actualizarPreinscripcion(
                        creada.getMatriculaId(), datos));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void regeneraChecklistAlCambiarElTipoDeIngresoConservandoRequisitosAunAplicables() {
        Seccion seccion = crearSeccion(2, "R");
        PreinscripcionResponseDTO creada = preinscripcionService.preinscribir(
                crearRequest(seccion.getId(), "EST-EDITAR-06", "REP-EDITAR-06"));
        Matricula matricula = matriculaRepository.findById(creada.getMatriculaId()).orElseThrow();
        matricula.setEstadoMatricula("en_proceso");
        matriculaRepository.saveAndFlush(matricula);

        var nivel = seccion.getGrado().getNivelEducativo();
        RequisitoMatricula comun = crearRequisito("REQUISITO-COMUN-EDITAR", nivel, null);
        RequisitoMatricula soloNuevo = crearRequisito(
                "REQUISITO-NUEVO-EDITAR", nivel, "nuevo_ingreso");
        RequisitoMatricula soloTraslado = crearRequisito(
                "REQUISITO-TRASLADO-EDITAR", nivel, "traslado");
        ChecklistMatricula itemComun = crearChecklist(matricula, comun, true);
        crearChecklist(matricula, soloNuevo, true);

        PreinscripcionRequestDTO datos = preinscripcionService
                .obtenerDatosPreinscripcion(matricula.getId());
        datos.setInstitucionProcedencia("Escuela de origen");
        preinscripcionService.actualizarPreinscripcion(matricula.getId(), datos);
        entityManager.flush();
        entityManager.clear();

        List<ChecklistMatricula> actualizados = checklistRepository
                .findByMatricula_Id(matricula.getId());
        assertEquals(1, actualizados.stream()
                .filter(item -> item.getRequisito().getId().equals(comun.getId())
                        && Boolean.TRUE.equals(item.getCumplido())
                        && item.getId().equals(itemComun.getId()))
                .count());
        assertEquals(0, actualizados.stream()
                .filter(item -> item.getRequisito().getId().equals(soloNuevo.getId()))
                .count());
        assertEquals(1, actualizados.stream()
                .filter(item -> item.getRequisito().getId().equals(soloTraslado.getId())
                        && Boolean.FALSE.equals(item.getCumplido()))
                .count());
    }

    private RequisitoMatricula crearRequisito(String nombre,
                                               NivelEducativo nivel,
                                               String aplicaTipoIngreso) {
        RequisitoMatricula requisito = new RequisitoMatricula();
        requisito.setNombre(nombre);
        requisito.setObligatorio(true);
        requisito.setActivo(true);
        requisito.setNivelEducativo(nivel);
        requisito.setAplicaTipoIngreso(aplicaTipoIngreso);
        return requisitoRepository.saveAndFlush(requisito);
    }

    private ChecklistMatricula crearChecklist(Matricula matricula,
                                               RequisitoMatricula requisito,
                                               boolean cumplido) {
        ChecklistMatricula item = new ChecklistMatricula();
        item.setMatricula(matricula);
        item.setRequisito(requisito);
        item.setCumplido(cumplido);
        return checklistRepository.saveAndFlush(item);
    }

    private Matricula crearMatriculaEnSeccion(Estudiante estudiante,
                                               Seccion seccion,
                                               String estado) {
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar(seccion.getAnioEscolar());
        matricula.setFechaSolicitud(LocalDate.now(clock));
        matricula.setEstadoMatricula(estado);
        return matriculaRepository.saveAndFlush(matricula);
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
                nuevoGrado.setEdadMinima((short) 6);
                nuevoGrado.setEdadMaxima((short) 7);
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
        estudiante.setCedula(cedulaEstudianteDeClave(cedula));
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
        estudiante.setCedula(cedulaEstudianteDeClave(cedulaEstudiante));
        estudiante.setTieneCedulaIdentidad(true);
        estudiante.setNombre("Ana");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2020, 1, 1));
        estudiante.setSexo("F");
        request.setEstudiante(estudiante);

        RepresentanteData representante = new RepresentanteData();
        representante.setCedula(cedulaRepresentanteDeClave(cedulaRepresentante));
        representante.setNombre("Nombre");
        representante.setApellido("Apellido");
        representante.setParentesco("Madre");
        representante.setCorreo("representante@example.com");
        representante.setTelefono("04121234567");
        request.setRepresentante(representante);
        return request;
    }

    private String cedulaRepresentanteDeClave(String clave) {
        int numero = 60_000_000 + Math.floorMod(clave.hashCode(), 20_000_000);
        return String.format(Locale.ROOT, "V-%08d", numero);
    }

    private String cedulaEstudianteDeClave(String clave) {
        int numero = 60_000_000 + Math.floorMod(clave.hashCode(), 20_000_000);
        return String.format(Locale.ROOT, "V-%08d", numero);
    }
}