package com.tesis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
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