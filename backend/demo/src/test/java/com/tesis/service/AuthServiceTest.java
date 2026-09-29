package com.tesis.service;

import com.tesis.dto.AuthDTO;
import com.tesis.entity.Roles;
import com.tesis.entity.TipoUsuario;
import com.tesis.entity.Usuario;
import com.tesis.exception.AuthenticationException;
import com.tesis.exception.UsuarioNotFoundException;
import com.tesis.mapper.UsuarioMapper;
import com.tesis.repository.UsuarioRepository;
import com.tesis.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UsuarioMapper usuarioMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtProvider, usuarioMapper);
    }

    @Test
    void loginExitosoDevuelveTokenYActualizaUltimoAcceso() {
        Usuario usuario = usuarioActivo();
        AuthDTO.LoginRequest solicitud = AuthDTO.LoginRequest.builder()
                .email("ana@example.com")
                .password("clave")
                .build();

        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "hash")).thenReturn(true);
        when(jwtProvider.generarToken(7, "ana@example.com", "ana", "Administrativo"))
            .thenReturn("jwt-de-prueba");

        AuthDTO.LoginResponse respuesta = authService.login(solicitud);

        assertEquals("jwt-de-prueba", respuesta.getToken());
        assertEquals(7, respuesta.getUsuarioId());
        assertEquals("ana@example.com", respuesta.getEmail());
        assertEquals("Administrativo", respuesta.getRolNombre());
        assertEquals(TipoUsuario.PERSONAL.toString(), respuesta.getTipoUsuario());
        assertTrue(respuesta.getActivo());
        assertNotNull(usuario.getUltimoAcceso());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void loginConDatosVaciosFallaSinConsultarRepositorio() {
        AuthDTO.LoginRequest solicitud = AuthDTO.LoginRequest.builder()
                .email("")
                .password("clave")
                .build();

        assertThrows(AuthenticationException.class, () -> authService.login(solicitud));

        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtProvider);
    }

    @Test
    void loginConUsuarioInexistenteFalla() {
        AuthDTO.LoginRequest solicitud = AuthDTO.LoginRequest.builder()
                .email("ausente@example.com")
                .password("clave")
                .build();
        when(usuarioRepository.findByEmail("ausente@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authService.login(solicitud));

        verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void loginConUsuarioInactivoFallaAntesDeValidarContrasena() {
        Usuario usuario = usuarioActivo();
        usuario.setActivo(false);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(AuthenticationException.class, () -> authService.login(solicitudLogin()));

        verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void loginConContrasenaIncorrectaFalla() {
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuarioActivo()));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);
        AuthDTO.LoginRequest solicitud = AuthDTO.LoginRequest.builder()
                .email("ana@example.com")
                .password("incorrecta")
                .build();

        assertThrows(AuthenticationException.class, () -> authService.login(solicitud));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void registroConEmailExistenteFalla() {
        AuthDTO.RegisterRequest solicitud = AuthDTO.RegisterRequest.builder()
                .nombreUsuario("ana")
                .email("ana@example.com")
                .password("clave")
                .build();
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(AuthenticationException.class, () -> authService.registrar(solicitud));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any(Usuario.class));
        verify(usuarioRepository, never()).existsByNombreUsuario("ana");
    }

    @Test
    void registroConNombreDeUsuarioExistenteFalla() {
        AuthDTO.RegisterRequest solicitud = AuthDTO.RegisterRequest.builder()
                .nombreUsuario("ana")
                .email("ana@example.com")
                .password("clave")
                .build();
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(true);

        assertThrows(AuthenticationException.class, () -> authService.registrar(solicitud));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any(Usuario.class));
    }

    private AuthDTO.LoginRequest solicitudLogin() {
        return AuthDTO.LoginRequest.builder()
                .email("ana@example.com")
                .password("clave")
                .build();
    }

    private Usuario usuarioActivo() {
        Roles rol = Roles.builder()
                .id(2)
                .nombreRol("Administrativo")
                .activo(true)
                .build();
        return Usuario.builder()
                .id(7)
                .nombreUsuario("ana")
                .email("ana@example.com")
                .passwordHash("hash")
                .rol(rol)
                .tipoUsuario(TipoUsuario.PERSONAL)
                .activo(true)
                .build();
    }
}