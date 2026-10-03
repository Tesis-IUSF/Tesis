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