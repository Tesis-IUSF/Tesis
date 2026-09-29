package com.tesis.service;

import com.tesis.dto.AuthDTO;
import com.tesis.dto.UsuarioDTO;
import com.tesis.entity.Usuario;
import com.tesis.exception.AuthenticationException;
import com.tesis.exception.UsuarioNotFoundException;
import com.tesis.mapper.UsuarioMapper;
import com.tesis.repository.UsuarioRepository;
import com.tesis.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UsuarioMapper usuarioMapper;

    /**
     * Realiza el login de un usuario
     */
    @Transactional
    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest loginRequest) {
        log.info("Intento de login para el email: {}", loginRequest.getEmail());

        // Validar que el email y contraseña no sean nulos
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isEmpty() ||
            loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            throw new AuthenticationException("Email y contraseña son requeridos");
        }

        // Buscar el usuario por email
        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new AuthenticationException("Email o contraseña incorrectos"));

        // Verificar que el usuario esté activo
        if (!usuario.getActivo()) {
            log.warn("Intento de login con usuario inactivo: {}", loginRequest.getEmail());
            throw new AuthenticationException("El usuario está inactivo");
        }

        // Verificar la contraseña
        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
            log.warn("Contraseña incorrecta para el usuario: {}", loginRequest.getEmail());
            throw new AuthenticationException("Email o contraseña incorrectos");
        }

        // Generar el token JWT
        String token = generarToken(usuario);

        // Actualizar el último acceso
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("Login exitoso para el usuario: {}", loginRequest.getEmail());

        // Construir la respuesta
        return AuthDTO.LoginResponse.builder()
                .token(token)
                .usuarioId(usuario.getId())
                .nombreUsuario(usuario.getNombreUsuario())
                .email(usuario.getEmail())
                .rolNombre(usuario.getRol().getNombreRol())
                .tipoUsuario(usuario.getTipoUsuario().toString())
                .activo(usuario.getActivo())
                .build();
    }

    /**
     * Genera un token JWT para un usuario
     */
    public String generarToken(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new AuthenticationException("Usuario inválido para generar token");
        }
        
        return jwtProvider.generarToken(
                usuario.getId(),
                usuario.getEmail(),
            usuario.getNombreUsuario(),
                usuario.getRol() == null ? null : usuario.getRol().getNombreRol()
        );
    }

    /**
     * Valida un token JWT
     */
    public boolean validarToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return jwtProvider.esTokenValido(token);
    }

    /**
     * Obtiene información del token validado
     */
    public AuthDTO.TokenValidationResponse validarTokenYObtenerInfo(String token) {
        try {
            Claims claims = jwtProvider.validarToken(token);
            
            Integer usuarioId = claims.get("usuarioId", Integer.class);
            String email = claims.getSubject();
            String nombreUsuario = claims.get("nombreUsuario", String.class);

            return AuthDTO.TokenValidationResponse.builder()
                    .valid(true)
                    .usuarioId(usuarioId)
                    .email(email)
                    .nombreUsuario(nombreUsuario)
                    .build();
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return AuthDTO.TokenValidationResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    /**
     * Registra un nuevo usuario
     */
    @Transactional
    public AuthDTO.LoginResponse registrar(AuthDTO.RegisterRequest registerRequest) {
        log.info("Intento de registro para el email: {}", registerRequest.getEmail());

        // Validar que no exista un usuario con el mismo email
        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new AuthenticationException("El email ya está registrado");
        }

        // Validar que no exista un usuario con el mismo nombre de usuario
        if (usuarioRepository.existsByNombreUsuario(registerRequest.getNombreUsuario())) {
            throw new AuthenticationException("El nombre de usuario ya está en uso");
        }

        // Crear el nuevo usuario
        Usuario nuevoUsuario = Usuario.builder()
                .nombreUsuario(registerRequest.getNombreUsuario())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .activo(true)
                .emailVerificado(false)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // Generar token
        String token = generarToken(usuarioGuardado);

        log.info("Registro exitoso para el usuario: {}", registerRequest.getEmail());

        return AuthDTO.LoginResponse.builder()
                .token(token)
                .usuarioId(usuarioGuardado.getId())
                .nombreUsuario(usuarioGuardado.getNombreUsuario())
                .email(usuarioGuardado.getEmail())
                .activo(usuarioGuardado.getActivo())
                .build();
    }

    /**
     * Obtiene el usuario actual desde el token
     */
    public UsuarioDTO.UsuarioResponseDTO obtenerUsuarioDelToken(String token) {
        try {
            Integer usuarioId = jwtProvider.obtenerUsuarioIdDelToken(token);
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado"));
            return usuarioMapper.usuarioToUsuarioResponseDTO(usuario);
        } catch (Exception e) {
            log.error("Error obteniendo usuario del token: {}", e.getMessage());
            throw new AuthenticationException("Token inválido o expirado");
        }
    }

    /**
     * Cierra sesión (lógica simple, el token expira naturalmente)
     */
    public void logout(String token) {
        log.info("Logout realizado");
        // En esta implementación simple, el logout solo es informativo
        // El token expirará automáticamente después de 24 horas
    }
}