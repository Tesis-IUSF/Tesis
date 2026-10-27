package com.tesis.controller;

import com.tesis.entity.TipoUsuario;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InitialSetupControllerTest {

    private static final String SETUP_TOKEN =
            "test-initial-admin-token-with-more-than-32-characters";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolesRepository rolesRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void estadoIndicaConfiguracionPendienteYRolesBaseDisponibles() throws Exception {
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialSetupRequired").value(true))
                .andExpect(jsonPath("$.setupTokenConfigured").value(true));

        assertThat(rolesRepository.findByNombreRol("ADMINISTRADOR")).isPresent();
        assertThat(rolesRepository.findByNombreRol("DIRECTOR")).isPresent();
        assertThat(rolesRepository.findByNombreRol("COORDINADOR")).isPresent();
        assertThat(rolesRepository.findByNombreRol("ADMINISTRATIVO")).isPresent();
        assertThat(rolesRepository.findByNombreRol("ESCANER")).isPresent();
    }

    @Test
    void creaAdministradorActivoConRolYContrasenaCifrada() throws Exception {
        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", SETUP_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "admin.inicial",
                                  "email": "ADMIN@ejemplo.com",
                                  "password": "UnaClaveSegura-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("admin.inicial"))
                .andExpect(jsonPath("$.email").value("admin@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));

        var admin = usuarioRepository.findByEmail("admin@ejemplo.com").orElseThrow();
        assertThat(admin.getActivo()).isTrue();
        assertThat(admin.getTipoUsuario()).isEqualTo(TipoUsuario.PERSONAL);
        assertThat(admin.getRol().getNombreRol()).isEqualTo("ADMINISTRADOR");
        assertThat(admin.getPasswordHash()).isNotEqualTo("UnaClaveSegura-123");
    }

    @Test
    void rechazaContrasenaComunEnElAltaInicial() throws Exception {
        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", SETUP_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "gestor.inicial",
                                  "email": "gestor@ejemplo.com",
                                  "password": "Password2026!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noPermiteUnSegundoAltaInicial() throws Exception {
        String request = """
                {
                  "nombreUsuario": "admin.inicial",
                  "email": "admin@ejemplo.com",
                  "password": "UnaClaveSegura-123"
                }
                """;

        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", SETUP_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", SETUP_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaTokenIncorrectoYRegistroPublicoAnterior() throws Exception {
        String request = """
                {
                  "nombreUsuario": "admin.inicial",
                  "email": "admin@ejemplo.com",
                  "password": "UnaClaveSegura-123"
                }
                """;

        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", "token-incorrecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void validaLosDatosDelAdministrador() throws Exception {
        mockMvc.perform(post("/api/setup/initial-admin")
                        .header("X-Initial-Admin-Token", SETUP_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "ad",
                                  "email": "no-es-email",
                                  "password": "corta"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
