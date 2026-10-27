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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void permiteGestionarDepartamentosYCargosSoloAlAdministrador() throws Exception {
        for (String recurso : new String[]{
                "/api/catalogos/departamentos", "/api/catalogos/cargos"
        }) {
            mockMvc.perform(post(recurso)
                            .with(user("director").roles("DIRECTOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(recurso)
                            .with(user("administrador").roles("ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get(recurso)
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isOk());
            mockMvc.perform(get(recurso)
                            .with(user("administrador").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk());
            mockMvc.perform(put(recurso + "/1")
                            .with(user("director").roles("DIRECTOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(recurso + "/1")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void restringeLaGestionDeCatalogosYPermiteLecturaAlCoordinador() throws Exception {
        for (String recurso : new String[]{
                "/api/catalogos/departamentos",
                "/api/catalogos/cargos",
                "/api/catalogos/turnos"
        }) {
            mockMvc.perform(get(recurso)
                            .with(user("coordinador").roles("COORDINADOR")))
                    .andExpect(status().isOk());
            mockMvc.perform(post(recurso)
                            .with(user("coordinador").roles("COORDINADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        for (String recurso : new String[]{
                "/api/catalogos/departamentos",
                "/api/catalogos/cargos"
        }) {
            mockMvc.perform(put(recurso + "/1")
                            .with(user("coordinador").roles("COORDINADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(recurso + "/1")
                            .with(user("coordinador").roles("COORDINADOR")))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/catalogos/turnos")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/catalogos/turnos")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/catalogos/turnos")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restringeLaGestionDeGradosYSeccionesAlAdministrador() throws Exception {
        for (String recurso : new String[]{"/api/catalogos/grados", "/api/catalogos/secciones"}) {
            mockMvc.perform(get(recurso)
                            .with(user("administrativo").roles("ADMINISTRATIVO")))
                    .andExpect(status().isOk());

            mockMvc.perform(post(recurso)
                            .with(user("director").roles("DIRECTOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(recurso)
                            .with(user("admin").roles("ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            for (String rol : new String[]{"COORDINADOR", "ADMINISTRATIVO"}) {
                mockMvc.perform(post(recurso)
                                .with(user(rol.toLowerCase()).roles(rol))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isForbidden());
                mockMvc.perform(put(recurso + "/1")
                                .with(user(rol.toLowerCase()).roles(rol))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isForbidden());
                mockMvc.perform(delete(recurso + "/1")
                                .with(user(rol.toLowerCase()).roles(rol)))
                        .andExpect(status().isForbidden());
            }
        }

        mockMvc.perform(get("/api/catalogos/docentes")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/catalogos/docentes")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/catalogos/docentes")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/catalogos/requisitos-matricula")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/catalogos/requisitos-matricula")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void limitaGestionDeEmpleadosAlCoordinadorYPermiteConsultaAlDirector() throws Exception {
        mockMvc.perform(get("/api/empleados")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/empleados")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/empleados")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/empleados")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/empleados")
                        .with(user("coordinador").roles("COORDINADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limitaCarnetsAlCoordinador() throws Exception {
        mockMvc.perform(get("/api/empleados/carnets")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/empleados/carnets")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteAlDirectorConsultarAsistenciaEHistorico() throws Exception {
        mockMvc.perform(get("/api/asistencias/hoy")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/asistencias/hoy")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/asistencias/historico")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/asistencias/historico")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void restringeAdministracionDeUsuariosAlAdministrador() throws Exception {
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
    void permiteAdministracionDeUsuariosSoloAlAdministrador() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .with(user("usuario").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios")
                        .with(user("usuario").roles("DIRECTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void restringeAdministracionDelBlogADirectorYCoordinador() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR", "ADMINISTRATIVO", "ESCANER"}) {
            mockMvc.perform(get("/api/blog/admin/categorias")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/blog/admin/categorias")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/blog/admin/categorias/1")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/blog/admin/categorias/1")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden());
        }

        for (String rol : new String[]{"DIRECTOR", "COORDINADOR"}) {
            mockMvc.perform(get("/api/blog/admin/categorias")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void aplicaPermisosDeLecturaYEscrituraDeMatriculaPorRol() throws Exception {
        for (String rol : new String[]{
                "ADMINISTRADOR", "DIRECTOR", "COORDINADOR", "ADMINISTRATIVO"
        }) {
            mockMvc.perform(get("/api/matriculas/gestion/anios")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isOk());
        }

        for (String rol : new String[]{"COORDINADOR", "ADMINISTRATIVO"}) {
            mockMvc.perform(post("/api/matriculas/999999/checklist")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/matriculas/preinscripciones")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/matriculas/999999/checklist")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/matriculas/1/checklist")
                        .with(user("director").roles("DIRECTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/matriculas/999/retiros")
                        .with(user("coordinador").roles("COORDINADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        for (String rol : new String[]{
                "ADMINISTRADOR", "DIRECTOR", "ADMINISTRATIVO"
        }) {
            mockMvc.perform(post("/api/matriculas/999/retiros")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void permiteGestionarCalendarioEscolarSoloAlAdministrador() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR"}) {
            mockMvc.perform(post("/api/matriculas/gestion/anios")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(put("/api/matriculas/gestion/anios/2027")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                            .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/matriculas/gestion/anios/2027/activar")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isNotFound());

            mockMvc.perform(post("/api/matriculas/gestion/anios/2027/periodos")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(put("/api/matriculas/gestion/periodos/999999")
                            .with(user("usuario").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(patch("/api/matriculas/gestion/periodos/999999/activar")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/api/matriculas/gestion/periodos/999999")
                            .with(user("usuario").roles(rol)))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(post("/api/matriculas/gestion/anios")
                        .with(user("coordinador").roles("COORDINADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/matriculas/gestion/anios")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/matriculas/gestion/anios/2027/periodos")
                        .with(user("administrativo").roles("ADMINISTRATIVO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/matriculas/gestion/egresos/999999/resolver")
                        .with(user("director").roles("DIRECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaCargaDeFotosParaRolEscaner() throws Exception {
        mockMvc.perform(multipart("/api/fotos")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[]{1}))
                        .with(user("escaner").roles("ESCANER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteCargaDeFotosParaRolCoordinador() throws Exception {
        mockMvc.perform(multipart("/api/fotos")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[]{1}))
                        .with(user("coordinador").roles("COORDINADOR")))
                .andExpect(status().isBadRequest());
    }

        @Test
        void rechazaEmisionDeCarnetParaRolEscaner() throws Exception {
            mockMvc.perform(post("/api/empleados/1/carnet")
                            .with(user("escaner").roles("ESCANER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void soloCoordinadorPuedeSolicitarCarnet() throws Exception {
            mockMvc.perform(post("/api/empleados/999/carnet")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/empleados/999/carnet")
                            .with(user("coordinador").roles("COORDINADOR")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void soloCoordinadorPuedeDescargarCarnetsExistentes() throws Exception {
            mockMvc.perform(post("/api/empleados/carnets/lote/descarga")
                    .with(user("escaner").roles("ESCANER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"empleadoIds\":[1],\"formato\":\"pdf\"}"))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/empleados/carnets/lote/descarga")
                    .with(user("director").roles("DIRECTOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoIds\":[999],\"formato\":\"pdf\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/empleados/carnets/lote/descarga")
                    .with(user("coordinador").roles("COORDINADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoIds\":[999],\"formato\":\"pdf\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void consultaAsistenciasDeHoyDevuelveListaJson() throws Exception {
            mockMvc.perform(get("/api/asistencias/hoy")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void consultaHistoricoConFiltrosOpcionales() throws Exception {
            mockMvc.perform(get("/api/asistencias/historico")
                            .param("desde", "2026-09-01")
                            .param("hasta", "2026-09-29")
                            .param("estado", "tardanza")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void consultaAusenciasConRangoDeFechas() throws Exception {
            mockMvc.perform(get("/api/asistencias/ausencias")
                            .param("desde", "2026-09-01")
                            .param("hasta", "2026-09-29")
                            .with(user("director").roles("DIRECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
}