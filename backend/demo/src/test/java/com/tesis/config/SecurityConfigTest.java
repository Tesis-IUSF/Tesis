package com.tesis.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

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
    void rechazaEscaneoQrParaUnRolDistintoDeEscaner() throws Exception {
        mockMvc.perform(post("/api/asistencias/qr")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteEscaneoQrAlRolEscaner() throws Exception {
        mockMvc.perform(post("/api/asistencias/qr")
                        .with(user("escaner").roles("ESCANER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"test\"}"))
                    .andExpect(status().isUnauthorized());
    }

        @Test
        void rechazaEmisionDeCarnetParaRolEscaner() throws Exception {
            mockMvc.perform(post("/api/empleados/1/carnet")
                            .with(user("escaner").roles("ESCANER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void soloRolesAdministrativosPuedenSolicitarCarnet() throws Exception {
            mockMvc.perform(post("/api/empleados/999/carnet")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void consultaAsistenciasDeHoyDevuelveListaJson() throws Exception {
            mockMvc.perform(get("/api/asistencias/hoy")
                            .with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
}