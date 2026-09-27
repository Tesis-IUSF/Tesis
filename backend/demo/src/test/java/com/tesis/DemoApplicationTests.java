package com.tesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.dto.AuthDTO;
import com.tesis.dto.UsuarioDTO;
import com.tesis.entity.Roles;
import com.tesis.entity.Usuario;
import com.tesis.entity.TipoUsuario;
import com.tesis.exception.AuthenticationException;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import com.tesis.security.JwtProvider;
import com.tesis.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.jwt.secret=test_secret_key_super_segura_minimo_32_caracteres",
    "app.jwt.expiration=86400000"
})
class DemoApplicationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RolesRepository rolesRepository;

    @Autowired
    private AuthService authService;

    private Usuario usuarioTest;
    private Roles rolTest;
    private AuthDTO.LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Inicializar MockMvc manualmente desde el contexto de Spring
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Crear rol de prueba
        rolTest = Roles.builder()
                .id(1)
                .nombreRol("Admin")
                .descripcion("Administrador del sistema")
                .activo(true)
                .build();

        // Crear usuario de prueba
        usuarioTest = Usuario.builder()
                .id(1)
                .nombreUsuario("usuario_test")
                .email("test@ejemplo.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .rol(rolTest)
                .tipoUsuario(TipoUsuario.PERSONAL)
                .activo(true)
                .emailVerificado(false)
                .ultimoAcceso(null)
                .bloqueadoHasta(null)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();

        // Crear request de login
        loginRequest = AuthDTO.LoginRequest.builder()
                .email("test@ejemplo.com")
                .password("password123")
                .build();
    }

    // ============== TESTS DE CONTEXTO ==============

    @Test
    @DisplayName("El contexto de la aplicación debe cargar correctamente")
    void contextLoads() {
        // Este test verifica que el contexto se cargue sin errores
        assertThat(authService).isNotNull();
        assertThat(jwtProvider).isNotNull();
    }

    // ============== TESTS DE LOGIN ==============

    @Test
    @DisplayName("Login exitoso debe retornar un token válido")
    void loginExitoso() throws Exception {
        // Arrange
        when(usuarioRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(usuarioTest));

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.nombreUsuario").value("usuario_test"))
                .andExpect(jsonPath("$.email").value("test@ejemplo.com"))
                .andExpect(jsonPath("$.rolNombre").value("Admin"))
                .andReturn();

        // Validar que el token es válido
        String response = result.getResponse().getContentAsString();
        AuthDTO.LoginResponse loginResponse = objectMapper.readValue(response, AuthDTO.LoginResponse.class);
        assertThat(jwtProvider.esTokenValido(loginResponse.getToken())).isTrue();
    }

    @Test
    @DisplayName("Login con email inexistente debe lanzar excepción")
    void loginConEmailInexistente() throws Exception {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta debe fallar")
    void loginConContraseñaIncorrecta() throws Exception {
        // Arrange
        when(usuarioRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(usuarioTest));

        AuthDTO.LoginRequest loginIncorrecto = AuthDTO.LoginRequest.builder()
                .email("test@ejemplo.com")
                .password("passwordIncorrecto123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginIncorrecto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login con usuario inactivo debe fallar")
    void loginConUsuarioInactivo() throws Exception {
        // Arrange
        usuarioTest.setActivo(false);
        when(usuarioRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(usuarioTest));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login sin email debe retornar error")
    void loginSinEmail() throws Exception {
        // Arrange
        AuthDTO.LoginRequest loginSinEmail = AuthDTO.LoginRequest.builder()
                .email("")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginSinEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login sin contraseña debe retornar error")
    void loginSinContraseña() throws Exception {
        // Arrange
        AuthDTO.LoginRequest loginSinPassword = AuthDTO.LoginRequest.builder()
                .email("test@ejemplo.com")
                .password("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginSinPassword)))
                .andExpect(status().isBadRequest());
    }

    // ============== TESTS DE VALIDACIÓN DE TOKENS ==============

    @Test
    @DisplayName("Token válido debe pasar validación")
    void tokenValidoEsValido() throws Exception {
        // Arrange
        String token = jwtProvider.generarToken(usuarioTest.getId(), usuarioTest.getEmail(), usuarioTest.getNombreUsuario());

        // Act & Assert
        mockMvc.perform(get("/api/auth/validar")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.email").value("test@ejemplo.com"));
    }

    @Test
    @DisplayName("Token inválido debe fallar validación")
    void tokenInvalidoFallaValidacion() throws Exception {
        // Arrange
        String tokenInvalido = "token_invalido_xyz";

        // Act & Assert
        mockMvc.perform(get("/api/auth/validar")
                .header("Authorization", "Bearer " + tokenInvalido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("Token sin Bearer debe fallar")
    void tokenSinBearer() throws Exception {
        // Arrange
        String token = jwtProvider.generarToken(usuarioTest.getId(), usuarioTest.getEmail(), usuarioTest.getNombreUsuario());

        // Act & Assert
        mockMvc.perform(get("/api/auth/validar")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ============== TESTS DE REGISTRO ==============

    @Test
    @DisplayName("Registro exitoso de nuevo usuario")
    void registroExitoso() throws Exception {
        // Arrange
        AuthDTO.RegisterRequest registerRequest = AuthDTO.RegisterRequest.builder()
                .nombreUsuario("nuevo_usuario")
                .email("nuevo@ejemplo.com")
                .password("password123")
                .rolId(1)
                .build();

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(2);
            return usuario;
        });

        // Act & Assert
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nombreUsuario").value("nuevo_usuario"))
                .andExpect(jsonPath("$.email").value("nuevo@ejemplo.com"));
    }

    @Test
    @DisplayName("Registro con email duplicado debe fallar")
    void registroConEmailDuplicado() throws Exception {
        // Arrange
        AuthDTO.RegisterRequest registerRequest = AuthDTO.RegisterRequest.builder()
                .nombreUsuario("otro_usuario")
                .email("test@ejemplo.com")
                .password("password123")
                .rolId(1)
                .build();

        when(usuarioRepository.existsByEmail("test@ejemplo.com")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registro con nombre de usuario duplicado debe fallar")
    void registroConNombreUsuarioDuplicado() throws Exception {
        // Arrange
        AuthDTO.RegisterRequest registerRequest = AuthDTO.RegisterRequest.builder()
                .nombreUsuario("usuario_test")
                .email("otro@ejemplo.com")
                .password("password123")
                .rolId(1)
                .build();

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("usuario_test")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    // ============== TESTS DE LOGOUT ==============

    @Test
    @DisplayName("Logout debe retornar estado OK")
    void logoutExitoso() throws Exception {
        // Arrange
        String token = jwtProvider.generarToken(usuarioTest.getId(), usuarioTest.getEmail(), usuarioTest.getNombreUsuario());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ============== TESTS DE SERVICIO ==============

    @Test
    @DisplayName("Generar token desde servicio debe crear token válido")
    void generarTokenDesdeServicio() {
        // Act
        String token = authService.generarToken(usuarioTest);

        // Assert
        assertThat(token).isNotEmpty();
        assertThat(jwtProvider.esTokenValido(token)).isTrue();
        assertThat(jwtProvider.obtenerUsuarioIdDelToken(token)).isEqualTo(1);
    }

    @Test
    @DisplayName("Validar token desde servicio debe retornar true para token válido")
    void validarTokenDesdeServicio() {
        // Arrange
        String token = jwtProvider.generarToken(usuarioTest.getId(), usuarioTest.getEmail(), usuarioTest.getNombreUsuario());

        // Act
        boolean esValido = authService.validarToken(token);

        // Assert
        assertThat(esValido).isTrue();
    }

    @Test
    @DisplayName("Validar token desde servicio debe retornar false para token inválido")
    void validarTokenInvalidoDesdeServicio() {
        // Act
        boolean esValido = authService.validarToken("token_invalido");

        // Assert
        assertThat(esValido).isFalse();
    }

    @Test
    @DisplayName("Login en servicio debe actualizar ultimo acceso")
    void loginActualizaUltimoAcceso() {
        // Arrange
        when(usuarioRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(usuarioTest));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthDTO.LoginResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.getToken()).isNotEmpty();
        assertThat(response.getUsuarioId()).isEqualTo(1);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Login con usuario nulo debe lanzar excepción")
    void loginConUsuarioNuloLanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("Validar token y obtener información")
    void validarTokenYObtenerInfo() {
        // Arrange
        String token = jwtProvider.generarToken(usuarioTest.getId(), usuarioTest.getEmail(), usuarioTest.getNombreUsuario());

        // Act
        AuthDTO.TokenValidationResponse response = authService.validarTokenYObtenerInfo(token);

        // Assert
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUsuarioId()).isEqualTo(1);
        assertThat(response.getEmail()).isEqualTo("test@ejemplo.com");
        assertThat(response.getNombreUsuario()).isEqualTo("usuario_test");
    }

    @Test
    @DisplayName("Validar token inválido debe retornar valid=false")
    void validarTokenInvalidoRetornaFalse() {
        // Act
        AuthDTO.TokenValidationResponse response = authService.validarTokenYObtenerInfo("token_invalido");

        // Assert
        assertThat(response.isValid()).isFalse();
    }

}