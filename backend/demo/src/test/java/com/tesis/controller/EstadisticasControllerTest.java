package com.tesis.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class EstadisticasControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void directorPuedeConsultarEstadisticasGenerales() throws Exception {
        mockMvc.perform(get("/api/director/estadisticas")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuposDisponibles").isNumber())
                .andExpect(jsonPath("$.cuposOcupados").isNumber())
                .andExpect(jsonPath("$.porcentajeOcupacion").isNumber())
                .andExpect(jsonPath("$.estudiantesPorSexo").isMap())
                .andExpect(jsonPath("$.estudiantesPorSeccion").isArray())
                .andExpect(jsonPath("$.nuevoIngreso").isNumber())
                .andExpect(jsonPath("$.regularesPendientes").isNumber());
    }

    @Test
    void rechazaLaConsultaDeEstadisticasParaRolesDistintosDeDirector() throws Exception {
        mockMvc.perform(get("/api/director/estadisticas")
                        .with(user("administrador").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

        @Test
        void exportaEstadisticasGeneralesComoPdfSoloParaDirector() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/director/estadisticas/pdf")
                .with(user("director").roles("DIRECTOR")))
            .andExpect(status().isOk())
            .andReturn();

        assertTrue(resultado.getResponse().getContentType().startsWith(MediaType.APPLICATION_PDF_VALUE));
        assertTrue(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
            .contains("estadisticas-matricula.pdf"));
        assertTrue(new String(resultado.getResponse().getContentAsByteArray(), 0, 5).startsWith("%PDF-"));

        mockMvc.perform(get("/api/director/estadisticas/pdf")
                .with(user("administrador").roles("ADMINISTRADOR")))
            .andExpect(status().isForbidden());
        }
}