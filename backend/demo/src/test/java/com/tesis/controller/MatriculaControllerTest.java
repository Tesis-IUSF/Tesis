package com.tesis.controller;

import com.tesis.entity.AnioEscolar;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.RelacionEstudianteRepresentante;
import com.tesis.entity.Representante;
import com.tesis.entity.Seccion;
import com.tesis.entity.TipoPeriodoMatricula;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatriculaControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

        @Autowired
        private EntityManager entityManager;

        @Autowired
        private Clock clock;

        @Autowired
        private CacheManager cacheManager;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        cacheManager.getCache("calendarioMatricula").clear();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void listaPreinscripcionesYMatriculasEnProceso() throws Exception {
        mockMvc.perform(get("/api/matriculas/preinscripciones")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.page").value(0))
                        .andExpect(jsonPath("$.size").value(25));

        mockMvc.perform(get("/api/matriculas/en-proceso")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/matriculas/listado")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

        @Test
        void exponeAnioActivoYVentanasVigentesParaElFrontend() throws Exception {
                AnioEscolar anio = new AnioEscolar();
                anio.setAnio((short) 2026);
                anio.setNombre("2026-2027");
                anio.setFechaInicio(LocalDate.of(2026, 9, 9));
                anio.setFechaFin(LocalDate.of(2027, 7, 30));
                anio.setActivo(true);
                entityManager.persist(anio);

                PeriodoMatricula periodo = new PeriodoMatricula();
                periodo.setAnioEscolar((short) 2026);
                periodo.setTipo(TipoPeriodoMatricula.preinscripcion);
                periodo.setFechaInicio(LocalDate.now(clock).minusDays(1));
                periodo.setFechaFin(LocalDate.now(clock).plusDays(1));
                periodo.setActivo(true);
                entityManager.persist(periodo);
                entityManager.flush();

                mockMvc.perform(get("/api/matriculas/calendario")
                                                .with(user("admin").roles("ADMINISTRADOR")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.anioActivo.anio").value(2026))
                                .andExpect(jsonPath("$.anioConsultado.nombre").value("2026-2027"))
                                .andExpect(jsonPath("$.periodos[0].tipo").value("preinscripcion"))
                                .andExpect(jsonPath("$.periodos[0].vigente").value(true));
        }

    @Test
    void respondeConCalendarioVacioYMensajeCuandoNoHayAnioActivo() throws Exception {
        mockMvc.perform(get("/api/matriculas/calendario")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anioActivo").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.anioConsultado").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.periodos").isEmpty())
                .andExpect(jsonPath("$.mensaje")
                        .value("No hay un año escolar activo configurado"));
    }

    @Test
    void creaAnioEscolarEnPreparacionYExponeSuListaDePeriodos() throws Exception {
        mockMvc.perform(post("/api/matriculas/gestion/anios")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "anio": 2199,
                                  "nombre": "2199-2200",
                                  "fechaInicio": "2199-09-01",
                                  "fechaFin": "2200-07-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").value(2199))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.periodos").isArray());
    }

    @Test
    void activarAnioCierraPreinscripcionEInscripcionDelAnterior() throws Exception {
        AnioEscolar anterior = nuevoAnio((short) 2197, true);
        entityManager.persist(anterior);
        AnioEscolar siguiente = nuevoAnio((short) 2198, false);
        entityManager.persist(siguiente);

        PeriodoMatricula preinscripcionAnterior = nuevoPeriodo((short) 2197,
                TipoPeriodoMatricula.preinscripcion);
        PeriodoMatricula inscripcionAnterior = nuevoPeriodo((short) 2197,
                TipoPeriodoMatricula.inscripcion);
        PeriodoMatricula extemporaneaAnterior = nuevoPeriodo((short) 2197,
                TipoPeriodoMatricula.extemporanea);
        entityManager.persist(preinscripcionAnterior);
        entityManager.persist(inscripcionAnterior);
        entityManager.persist(extemporaneaAnterior);

        entityManager.persist(nuevoPeriodo((short) 2198, TipoPeriodoMatricula.preinscripcion));
        entityManager.persist(nuevoPeriodo((short) 2198, TipoPeriodoMatricula.inscripcion));
        entityManager.persist(nuevoPeriodo((short) 2198, TipoPeriodoMatricula.extemporanea));
        entityManager.flush();

        mockMvc.perform(post("/api/matriculas/gestion/anios/2198/activar")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        entityManager.refresh(anterior);
        entityManager.refresh(preinscripcionAnterior);
        entityManager.refresh(inscripcionAnterior);
        entityManager.refresh(extemporaneaAnterior);
        org.junit.jupiter.api.Assertions.assertFalse(anterior.getActivo());
        org.junit.jupiter.api.Assertions.assertFalse(preinscripcionAnterior.getActivo());
        org.junit.jupiter.api.Assertions.assertFalse(inscripcionAnterior.getActivo());
        org.junit.jupiter.api.Assertions.assertTrue(extemporaneaAnterior.getActivo());
    }

    private AnioEscolar nuevoAnio(short anio, boolean activo) {
        AnioEscolar escolar = new AnioEscolar();
        escolar.setAnio(anio);
        escolar.setNombre(anio + "-" + (anio + 1));
        escolar.setFechaInicio(LocalDate.of(anio, 9, 1));
        escolar.setFechaFin(LocalDate.of(anio + 1, 7, 15));
        escolar.setActivo(activo);
        return escolar;
    }

    private PeriodoMatricula nuevoPeriodo(short anio, TipoPeriodoMatricula tipo) {
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setAnioEscolar(anio);
        periodo.setTipo(tipo);
        periodo.setFechaInicio(LocalDate.of(anio, 5, 1));
        periodo.setFechaFin(LocalDate.of(anio, 5, 31));
        periodo.setActivo(true);
        return periodo;
    }

    @Test
    void rechazaAccesoDeUsuarioSinRolAdministrativo() throws Exception {
        mockMvc.perform(get("/api/matriculas/preinscripciones")
                        .with(user("estudiante").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

        @Test
        void aceptaParametrosDePaginaYLimitaElTamanoMaximo() throws Exception {
                mockMvc.perform(get("/api/matriculas/en-proceso")
                                                .param("page", "2")
                                                .param("size", "500")
                                                .with(user("admin").roles("ADMINISTRADOR")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.page").value(2))
                                .andExpect(jsonPath("$.size").value(100));
        }

        @Test
        void rechazaParametrosDePaginaInvalidos() throws Exception {
                mockMvc.perform(get("/api/matriculas/en-proceso")
                                                .param("page", "-1")
                                                .with(user("admin").roles("ADMINISTRADOR")))
                                .andExpect(status().isBadRequest());
        }

    @Test
        void buscaRepresentantesActivosPorElEndpointPaginado() throws Exception {
            Representante activo = new Representante();
            activo.setCedula("V-18456789");
            activo.setNombre("Ana");
            activo.setApellido("Pérez");
            activo.setCorreo("ana@example.com");
            activo.setTelefono("04121234567");
            entityManager.persist(activo);

            Representante inactivo = new Representante();
            inactivo.setCedula("V-18456790");
            inactivo.setNombre("Ana");
            inactivo.setApellido("Gómez");
            inactivo.setCorreo("ana2@example.com");
            inactivo.setTelefono("04121234568");
            inactivo.setActivo(false);
            entityManager.persist(inactivo);
            entityManager.flush();

            mockMvc.perform(get("/api/representantes/buscar")
                            .param("q", "Ana")
                            .param("page", "0")
                            .param("size", "1")
                            .with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(activo.getId()));
        }

        @Test
        void obtieneYActualizaDatosDePreinscripcionYExponeCedulaEnListado() throws Exception {
        MatriculaFixture fixture = crearMatriculaParaEdicion();
        Integer matriculaId = fixture.matriculaId();

        mockMvc.perform(get("/api/matriculas/{id}/preinscripcion", matriculaId)
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante.cedula").value("V-18456789"))
                .andExpect(jsonPath("$.estudiante.tieneCedulaIdentidad").value(true))
                .andExpect(jsonPath("$.representante.cedula").value("V-18456789"))
                .andExpect(jsonPath("$.representante.id").value(fixture.representanteId()))
                .andExpect(jsonPath("$.seccionId").value(fixture.seccionId()))
                .andExpect(jsonPath("$.anioEscolar").value(2026))
                .andExpect(jsonPath("$.relacionPrincipal").value(true))
                .andExpect(jsonPath("$.autorizadoRetirar").value(true))
                .andExpect(jsonPath("$.recibeComunicados").value(false));

        mockMvc.perform(get("/api/matriculas/{id}/expediente", matriculaId)
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matriculaId").value(matriculaId))
                .andExpect(jsonPath("$.estudiante.cedula").value("V-18456789"))
                .andExpect(jsonPath("$.nivelEducativoNombre").value("Nivel API edición"))
                .andExpect(jsonPath("$.letraSeccion").value("Z"))
                .andExpect(jsonPath("$.representantes.length()").value(1))
                .andExpect(jsonPath("$.representantes[0].id")
                        .value(fixture.representanteId()))
                .andExpect(jsonPath("$.representantes[0].relacionPrincipal").value(true));

        mockMvc.perform(get("/api/matriculas/{id}/historial-academico", matriculaId)
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matriculaId").value(matriculaId))
                .andExpect(jsonPath("$[0].estadoMatricula").value("preinscrito"))
                .andExpect(jsonPath("$[0].gradoNombre").value("1° grado"));

        mockMvc.perform(put("/api/matriculas/{id}/preinscripcion", matriculaId)
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "estudiante": {
                                    "cedula": "V-18456789",
                                    "tieneCedulaIdentidad": true,
                                    "nombre": "Nombre corregido",
                                    "apellido": "Estudiante",
                                    "fechaNacimiento": "2017-01-01",
                                    "sexo": "F"
                                  },
                                  "representante": {
                                    "id": %d,
                                    "cedula": "V-18456789",
                                    "nombre": "Representante corregido",
                                    "apellido": "Prueba",
                                    "parentesco": "Madre",
                                    "correo": "representante@example.com",
                                    "telefono": "04121234567"
                                  },
                                  "seccionId": %d,
                                  "anioEscolar": 2026,
                                  "institucionProcedencia": null,
                                  "relacionPrincipal": true,
                                  "autorizadoRetirar": true,
                                  "recibeComunicados": false
                                }
                                """, fixture.representanteId(), fixture.seccionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoMatricula").value("preinscrito"))
                .andExpect(jsonPath("$.estudianteNombreCompleto").value("Nombre corregido Estudiante"));

        mockMvc.perform(get("/api/matriculas/preinscripciones")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].estudianteCedula").value("V-18456789"));

        mockMvc.perform(get("/api/matriculas/listado")
                        .param("estado", "")
                        .param("q", "V-18456789")
                        .param("anioEscolar", "2026")
                        .param("gradoId", fixture.gradoId().toString())
                        .param("seccionId", fixture.seccionId().toString())
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].estudianteCedula").value("V-18456789"));

        mockMvc.perform(get("/api/matriculas/{id}/retiros/representantes-autorizados",
                        fixture.matriculaId())
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(fixture.representanteId()));

        mockMvc.perform(get("/api/matriculas/listado")
                        .param("estado", "no-es-valido")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaPosiblesGraduadosDelAnioActivoEnElUltimoGradoActivo() throws Exception {
        entityManager.createQuery("update AnioEscolar a set a.activo = false").executeUpdate();
        AnioEscolar anio = nuevoAnio((short) 2196, true);
        entityManager.persist(anio);

        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Nivel posibles graduados");
        nivel.setOrdinal((short) 198);
        entityManager.persist(nivel);

        Grado primero = nuevoGrado(nivel, (short) 1);
        Grado ultimo = nuevoGrado(nivel, (short) 2);
        entityManager.persist(primero);
        entityManager.persist(ultimo);
        entityManager.flush();

        crearMatriculaParaGraduacion(primero, (short) 2196, "V-21960001",
                "Fuera del último grado", "en_proceso", "A");
        FixtureGraduacion posible = crearMatriculaParaGraduacion(
                ultimo, (short) 2196, "V-21960002",
                "Posible Graduado", "en_proceso", "B");
        crearMatriculaParaGraduacion(ultimo, (short) 2196, "V-21960003",
                "Preinscrito no elegible", "preinscrito", "C");
        crearMatriculaParaGraduacion(ultimo, (short) 2195, "V-21950004",
                "Año anterior", "completada", "D");
        entityManager.flush();

        mockMvc.perform(get("/api/matriculas/posibles-graduados")
                        .param("q", "Posible Graduado")
                        .param("gradoId", ultimo.getId().toString())
                        .param("seccionId", posible.seccionId().toString())
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].estudianteNombreCompleto")
                        .value("Posible Graduado Prueba"))
                .andExpect(jsonPath("$.content[0].estadoMatricula").value("en_proceso"));
    }

    private MatriculaFixture crearMatriculaParaEdicion() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Nivel API edición");
        nivel.setOrdinal((short) 99);
        entityManager.persist(nivel);

        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        grado.setEdadMinima((short) 0);
        grado.setEdadMaxima((short) 120);
        entityManager.persist(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion("Z");
        seccion.setCapacidadMaxima((short) 10);
        seccion.setAnioEscolar((short) 2026);
        seccion.setActivo(true);
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula("V-18456789");
        estudiante.setNombre("Nombre anterior");
        estudiante.setApellido("Estudiante");
        estudiante.setFechaNacimiento(LocalDate.of(2017, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);

        Representante representante = new Representante();
        representante.setCedula("V-18456789");
        representante.setNombre("Representante anterior");
        representante.setApellido("Prueba");
        representante.setParentesco("Madre");
        representante.setCorreo("representante@example.com");
        representante.setTelefono("04121234567");
        entityManager.persist(representante);

        RelacionEstudianteRepresentante relacion = new RelacionEstudianteRepresentante();
        relacion.setEstudiante(estudiante);
        relacion.setRepresentante(representante);
        relacion.setRelacionPrincipal(true);
        relacion.setAutorizadoRetirar(true);
        relacion.setRecibeComunicados(false);
        relacion.setFechaVinculacion(LocalDate.now(clock));
        entityManager.persist(relacion);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setTipoIngreso("nuevo_ingreso");
        matricula.setEstadoMatricula("preinscrito");
        matricula.setFechaSolicitud(LocalDate.now(clock));
        entityManager.persist(matricula);
        entityManager.flush();
        return new MatriculaFixture(
                matricula.getId(), grado.getId(), seccion.getId(), representante.getId());
    }

    private Grado nuevoGrado(NivelEducativo nivel, Short numero) {
        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado(numero);
        grado.setEdadMinima((short) 0);
        grado.setEdadMaxima((short) 120);
        grado.setActivo(true);
        return grado;
    }

    private FixtureGraduacion crearMatriculaParaGraduacion(Grado grado,
                                                            Short anioEscolar,
                                                            String cedula,
                                                            String nombre,
                                                            String estado,
                                                            String letra) {
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion(letra);
        seccion.setCapacidadMaxima((short) 10);
        seccion.setAnioEscolar(anioEscolar);
        seccion.setActivo(true);
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombre(nombre);
        estudiante.setApellido("Prueba");
        estudiante.setFechaNacimiento(LocalDate.of(2010, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar(anioEscolar);
        matricula.setEstadoMatricula(estado);
        matricula.setFechaSolicitud(LocalDate.now(clock));
        entityManager.persist(matricula);
        return new FixtureGraduacion(seccion.getId());
    }

    private record MatriculaFixture(Integer matriculaId,
                                    Integer gradoId,
                                    Integer seccionId,
                                    Integer representanteId) {
    }

    @Test
    void descargaNominaEstudiantilSoloParaDirector() throws Exception {
        entityManager.persist(nuevoAnio((short) 2026, false));
        MatriculaFixture fixture = crearMatriculaParaEdicion();
        entityManager.flush();

        var resultado = mockMvc.perform(get("/api/matriculas/nomina-estudiantil/pdf")
                        .param("anioEscolar", "2026")
                        .param("estados", "preinscrito")
                        .param("gradoId", fixture.gradoId().toString())
                        .param("seccionId", fixture.seccionId().toString())
                        .param("columnas", "nombre", "cedula", "representanteNombre")
                        .param("q", "no debe filtrar")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(resultado.getResponse().getContentType().startsWith("application/pdf"));
        assertTrue(resultado.getResponse().getHeader("Content-Disposition")
                .contains("nomina-estudiantil-2026.pdf"));
        PdfReader reader = new PdfReader(resultado.getResponse().getContentAsByteArray());
        String contenido = new PdfTextExtractor(reader).getTextFromPage(1);
        assertTrue(contenido.contains("Nombre anterior"));
        assertTrue(contenido.contains("V-18456789"));
        assertTrue(contenido.contains("Representante anterior"));
        reader.close();

        mockMvc.perform(get("/api/matriculas/nomina-estudiantil/pdf")
                        .param("anioEscolar", "2026")
                        .param("columnas", "nombre")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    private record FixtureGraduacion(Integer seccionId) {
    }
}