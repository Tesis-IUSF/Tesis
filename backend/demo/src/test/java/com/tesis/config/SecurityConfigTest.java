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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                        .content("{\"empleadoId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteEscaneoQrAlRolEscaner() throws Exception {
        mockMvc.perform(post("/api/asistencias/qr")
                        .with(user("escaner").roles("ESCANER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":1}"))
                .andExpect(status().isNotFound());
    }
}