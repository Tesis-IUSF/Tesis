package com.tesis.config;

import com.tesis.entity.Roles;
import com.tesis.entity.TipoUsuario;
import com.tesis.entity.Usuario;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_ROLES = List.of(
            "Escaner",
            "Administrativo",
            "Administrador",
            "Director"
    );

    private final RolesRepository rolesRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email:admin.demo@asansa.local}")
    private String adminEmail;

    @Value("${app.seed.admin.username:admin.demo}")
    private String adminUsername;

    @Value("${app.seed.admin.password:AsansaDemo2026!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        DEFAULT_ROLES.forEach(this::ensureRoleExists);
        ensureDemoAdministratorExists();
    }

    private void ensureRoleExists(String roleName) {
        boolean exists = rolesRepository.findAll().stream()
                .anyMatch(role -> roleName.equalsIgnoreCase(role.getNombreRol()));
        if (!exists) {
            rolesRepository.save(Roles.builder()
                    .nombreRol(roleName)
                    .descripcion("Rol predeterminado de " + roleName)
                    .activo(true)
                    .build());
            log.info("Rol de desarrollo creado: {}", roleName);
        }
    }

    private void ensureDemoAdministratorExists() {
        if (usuarioRepository.existsByEmail(adminEmail)) {
            log.info("La cuenta demo ya existe: {}", adminEmail);
            return;
        }

        if (usuarioRepository.existsByNombreUsuario(adminUsername)) {
            log.warn("No se creó la cuenta demo: el nombre de usuario {} ya está ocupado", adminUsername);
            return;
        }

        Roles adminRole = rolesRepository.findAll().stream()
                .filter(role -> "Administrador".equalsIgnoreCase(role.getNombreRol()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo crear el rol Administrador"));

        usuarioRepository.save(Usuario.builder()
                .nombreUsuario(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(adminRole)
                .tipoUsuario(TipoUsuario.PERSONAL)
                .activo(true)
                .emailVerificado(true)
                .build());
        log.info("Cuenta administradora demo creada: {}", adminEmail);
    }
}