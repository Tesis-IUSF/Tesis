package com.tesis.controller;

import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
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
}