package com.tesis.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AsistenciaHistoricoControllerTest {

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
    void descargaReporteFiltradoSoloParaDirector() throws Exception {
        String hoy = LocalDate.now().toString();
        var resultado = mockMvc.perform(get("/api/asistencias/historico/pdf")
                        .param("desde", hoy)
                        .param("hasta", hoy)
                        .param("estado", "ausente")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(resultado.getResponse().getContentType().startsWith(MediaType.APPLICATION_PDF_VALUE));
        assertTrue(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)
                .contains("reporte-asistencia.pdf"));
        assertTrue(new String(resultado.getResponse().getContentAsByteArray(), 0, 5).startsWith("%PDF-"));

        mockMvc.perform(get("/api/asistencias/historico/pdf")
                        .param("desde", hoy)
                        .param("hasta", hoy)
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isForbidden());
    }
}
