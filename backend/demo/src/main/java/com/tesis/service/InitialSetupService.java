package com.tesis.service;

import com.tesis.dto.InitialSetupDTO.InitialAdminRequestDTO;
import com.tesis.dto.InitialSetupDTO.InitialAdminResponseDTO;
import com.tesis.dto.InitialSetupDTO.StatusResponseDTO;
import com.tesis.entity.Roles;
import com.tesis.entity.TipoUsuario;
import com.tesis.entity.Usuario;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class InitialSetupService {

    private static final String ADMIN_ROLE = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final byte[] configuredToken;

    public InitialSetupService(UsuarioRepository usuarioRepository,
                               RolesRepository rolesRepository,
                               BCryptPasswordEncoder passwordEncoder,
                               @Value("${app.setup.initial-admin-token:}") String setupToken) {
        this.usuarioRepository = usuarioRepository;
        this.rolesRepository = rolesRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredToken = setupToken.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public StatusResponseDTO obtenerEstado() {
        return new StatusResponseDTO(
                usuarioRepository.count() == 0,
                configuredToken.length >= 32);
    }

    @Transactional
    public InitialAdminResponseDTO crearAdministradorInicial(
            InitialAdminRequestDTO request, String suppliedToken) {
        validarTokenConfigurado(suppliedToken);

        Roles adminRole = rolesRepository.findByNombreRolForUpdate(ADMIN_ROLE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se encontraron los roles iniciales; reinicie la aplicación e intente de nuevo"));

        if (usuarioRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La configuración inicial ya fue completada");
        }
        if (!Boolean.TRUE.equals(adminRole.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol de administrador está inactivo");
        }

        String nombreUsuario = request.nombreUsuario().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)
                || usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El email o nombre de usuario ya está registrado");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La contraseña no puede superar 72 bytes UTF-8");
        }

        Usuario admin = new Usuario();
        admin.setNombreUsuario(nombreUsuario);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRol(adminRole);
        admin.setTipoUsuario(TipoUsuario.PERSONAL);
        admin.setActivo(true);
        admin = usuarioRepository.saveAndFlush(admin);

        return new InitialAdminResponseDTO(
                admin.getId(), admin.getNombreUsuario(), admin.getEmail(), ADMIN_ROLE);
    }

    private void validarTokenConfigurado(String suppliedToken) {
        if (configuredToken.length < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure INITIAL_ADMIN_SETUP_TOKEN con al menos 32 caracteres antes de crear el administrador");
        }
        if (suppliedToken == null || !MessageDigest.isEqual(
                configuredToken, suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token de configuración inicial inválido");
        }
    }
}
