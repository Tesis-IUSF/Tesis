package com.tesis.config;

import com.tesis.entity.Roles;
import com.tesis.repository.RolesRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RoleInitializer implements ApplicationRunner {

    private static final List<DefaultRole> DEFAULT_ROLES = List.of(
            new DefaultRole("ADMINISTRADOR", "Administración completa del sistema"),
            new DefaultRole("DIRECTOR", "Acceso directivo y gestión académica"),
            new DefaultRole("ADMINISTRATIVO", "Gestión administrativa de matrícula y personal"),
            new DefaultRole("ESCANER", "Registro de asistencia mediante QR"));

    private final RolesRepository rolesRepository;

    public RoleInitializer(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DefaultRole defaultRole : DEFAULT_ROLES) {
            if (rolesRepository.findByNombreRol(defaultRole.name()).isEmpty()) {
                Roles role = new Roles();
                role.setNombreRol(defaultRole.name());
                role.setDescripcion(defaultRole.description());
                role.setActivo(true);
                rolesRepository.save(role);
            }
        }
        rolesRepository.flush();
    }

    private record DefaultRole(String name, String description) {
    }
}
