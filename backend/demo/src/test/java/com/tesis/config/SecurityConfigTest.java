package com.tesis.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    void rechazaAsignacionDeTurnosParaRolEscaner() throws Exception {
        mockMvc.perform(post("/api/asignaciones-turnos")
                        .with(user("escaner").roles("ESCANER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaCreacionDeTurnoParaRolEscaner() throws Exception {
        mockMvc.perform(post("/api/catalogos/turnos")
                        .with(user("escaner").roles("ESCANER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restringeEscriturasDeDepartamentosCargosYRequisitosAAdminYDirector() throws Exception {
        for (String recurso : new String[]{
                "/api/catalogos/departamentos",
                "/api/catalogos/cargos",
                "/api/catalogos/requisitos-matricula"
        }) {
            mockMvc.perform(post(recurso)
                            .with(user("escaner").roles("ESCANER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(recurso + "/1")
                            .with(user("escaner").roles("ESCANER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(recurso + "/1")
                            .with(user("escaner").roles("ESCANER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void permiteEscriturasDeEsosCatalogosAAdminYDirector() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR", "DIRECTOR"}) {
            mockMvc.perform(post("/api/catalogos/departamentos")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void restringeAdministracionDeUsuariosAAdminYDirector() throws Exception {
        for (String rol : new String[]{"ESCANER", "ADMINISTRATIVO"}) {
            mockMvc.perform(get("/api/usuarios")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/usuarios")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/usuarios/1")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/usuarios/1")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void permiteAdministracionDeUsuariosAAdminYDirector() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR", "DIRECTOR"}) {
            mockMvc.perform(get("/api/usuarios")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void limitaGestionDeAnioEscolarYPeriodosAAdminYDirector() throws Exception {
        mockMvc.perform(get("/api/matriculas/gestion/anios")
                        .with(user("administrativo").roles("ADMINISTRATIVO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/matriculas/gestion/anios")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaCargaDeFotosParaRolEscaner() throws Exception {
        mockMvc.perform(multipart("/api/fotos")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[]{1}))
                        .with(user("escaner").roles("ESCANER")))
                .andExpect(status().isForbidden());
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
        void soloRolesAdministrativosPuedenDescargarCarnetsExistentes() throws Exception {
            mockMvc.perform(post("/api/empleados/carnets/lote/descarga")
                    .with(user("escaner").roles("ESCANER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"empleadoIds\":[1],\"formato\":\"pdf\"}"))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/empleados/carnets/lote/descarga")
                    .with(user("director").roles("DIRECTOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoIds\":[999],\"formato\":\"pdf\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void consultaAsistenciasDeHoyDevuelveListaJson() throws Exception {
            mockMvc.perform(get("/api/asistencias/hoy")
                            .with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void consultaHistoricoConFiltrosOpcionales() throws Exception {
            mockMvc.perform(get("/api/asistencias/historico")
                            .param("desde", "2026-09-01")
                            .param("hasta", "2026-09-29")
                            .param("estado", "tardanza")
                            .with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void consultaAusenciasConRangoDeFechas() throws Exception {
            mockMvc.perform(get("/api/asistencias/ausencias")
                            .param("desde", "2026-09-01")
                            .param("hasta", "2026-09-29")
                            .with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
}