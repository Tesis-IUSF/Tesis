package com.tesis.controller;

import com.tesis.entity.CategoriaComunicado;
import com.tesis.entity.Roles;
import com.tesis.entity.Usuario;
import com.tesis.repository.CategoriaComunicadoRepository;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogControllerTest {

    private static final String EMAIL_EDITOR = "editor@asansa.local";

    @Autowired
    private WebApplicationContext applicationContext;
    @Autowired
    private RolesRepository rolesRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaComunicadoRepository categoriaRepository;

    private MockMvc mockMvc;
    private CategoriaComunicado categoria;
    private Usuario editor;

    @BeforeEach
    void configurarDatosYMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        Roles rol = new Roles();
        rol.setNombreRol("Administrador blog");
        rol.setActivo(true);
        rolesRepository.save(rol);

        editor = new Usuario();
        editor.setNombreUsuario("editor.blog");
        editor.setEmail(EMAIL_EDITOR);
        editor.setPasswordHash("hash");
        editor.setRol(rol);
        usuarioRepository.save(editor);

        categoria = new CategoriaComunicado();
        categoria.setNombre("Noticias");
        categoria.setActivo(true);
        categoriaRepository.save(categoria);
    }

    @Test
    void permiteLecturaPublicaPeroRestringeLaAdministracion() throws Exception {
        mockMvc.perform(get("/api/blog/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Noticias"));

        mockMvc.perform(get("/api/blog/admin/categorias"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/blog/admin/categorias").with(user("lector").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/blog/admin/categorias")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void creaPublicaYRetiraUnComunicadoDeLaConsultaPublica() throws Exception {
        String request = """
                {
                  "titulo": "Inicio de clases",
                  "contenidoHtml": "<p>Las clases comienzan el lunes.</p>",
                  "resumen": "Calendario escolar",
                  "categoriaId": %d,
                  "publicado": true,
                  "visiblePublico": true
                }
                """.formatted(categoria.getId());

        MvcResult response = mockMvc.perform(post("/api/blog/admin/comunicados")
                        .with(user(EMAIL_EDITOR).roles("ADMINISTRATIVO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoPorId").value(editor.getId()))
                .andExpect(jsonPath("$.publicado").value(true))
                .andReturn();
        Integer comunicadoId = JsonPath.read(response.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/blog/publicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Inicio de clases"));

        mockMvc.perform(get("/api/blog/admin/comunicados")
                        .param("publicado", "true")
                        .with(user(EMAIL_EDITOR).roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(delete("/api/blog/admin/comunicados/" + comunicadoId)
                        .with(user(EMAIL_EDITOR).roles("ADMINISTRATIVO")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/blog/publicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
