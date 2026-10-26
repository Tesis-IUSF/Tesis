package com.tesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.tesis.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/auth/login", "/api/setup/status", "/api/setup/initial-admin").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/blog/categorias", "/api/blog/publicaciones/**").permitAll()
            .requestMatchers("/api/blog/admin/**")
                .hasAnyRole("DIRECTOR", "COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/asistencias/qr").hasRole("ESCANER")
            .requestMatchers(HttpMethod.GET, "/api/asistencias/**")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/fotos")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "ADMINISTRATIVO")
            .requestMatchers("/api/director/estadisticas", "/api/director/estadisticas/**")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/asistencias/empleado/*/salida")
                .hasAnyRole("ESCANER", "ADMINISTRADOR", "ADMINISTRATIVO")
            .requestMatchers("/api/asignaciones-turnos/**")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/catalogos/turnos")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.PUT, "/api/catalogos/turnos/*")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.DELETE, "/api/catalogos/turnos/*")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.GET, "/api/catalogos/turnos/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "COORDINADOR")
            .requestMatchers("/api/catalogos/turnos/**")
                .hasAnyRole("DIRECTOR", "COORDINADOR")
            .requestMatchers(HttpMethod.GET, "/api/catalogos/departamentos/**",
                    "/api/catalogos/cargos/**")
                .hasAnyRole("DIRECTOR", "COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/catalogos/departamentos",
                    "/api/catalogos/cargos")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.PUT, "/api/catalogos/departamentos/*",
                    "/api/catalogos/cargos/*")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.DELETE, "/api/catalogos/departamentos/*",
                    "/api/catalogos/cargos/*")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.GET, "/api/catalogos/docentes")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.GET, "/api/catalogos/grados/**",
                    "/api/catalogos/secciones/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "COORDINADOR", "ADMINISTRATIVO")
            .requestMatchers(HttpMethod.POST, "/api/catalogos/grados",
                    "/api/catalogos/secciones")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.PUT, "/api/catalogos/grados/*",
                    "/api/catalogos/secciones/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.DELETE, "/api/catalogos/grados/*",
                    "/api/catalogos/secciones/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/catalogos/requisitos-matricula")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.PUT, "/api/catalogos/requisitos-matricula/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.DELETE, "/api/catalogos/requisitos-matricula/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.GET, "/api/catalogos/requisitos-matricula/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "COORDINADOR", "ADMINISTRATIVO")
            .requestMatchers("/api/catalogos/requisitos-matricula/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers("/api/catalogos/departamentos/**", "/api/catalogos/cargos/**")
                .hasRole("DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/matriculas/gestion/anios",
                    "/api/matriculas/gestion/anios/*/activar",
                    "/api/matriculas/gestion/anios/*/periodos")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.PUT, "/api/matriculas/gestion/anios/*",
                    "/api/matriculas/gestion/periodos/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.PATCH,
                    "/api/matriculas/gestion/periodos/*/activar",
                    "/api/matriculas/gestion/periodos/*/desactivar")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.DELETE, "/api/matriculas/gestion/periodos/*")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/matriculas/*/retiros")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.GET, "/api/matriculas/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "COORDINADOR", "ADMINISTRATIVO")
            .requestMatchers("/api/matriculas/**")
                .hasAnyRole("ADMINISTRADOR", "COORDINADOR", "ADMINISTRATIVO")
            .requestMatchers(HttpMethod.POST, "/api/empleados/*/carnet")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.GET, "/api/empleados/carnets")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/empleados/carnets/lote")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/empleados/carnets/lote/descarga")
                .hasRole("COORDINADOR")
            .requestMatchers("/api/empleados/carnets/**")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.GET, "/api/empleados", "/api/empleados/*")
                .hasAnyRole("DIRECTOR", "COORDINADOR")
            .requestMatchers(HttpMethod.POST, "/api/empleados/**")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.PUT, "/api/empleados/**")
                .hasRole("COORDINADOR")
            .requestMatchers(HttpMethod.DELETE, "/api/empleados/**")
                .hasRole("COORDINADOR")
            .requestMatchers("/api/usuarios/**")
                .hasRole("ADMINISTRADOR")
            .anyRequest().authenticated())
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
            (request, response, exception) -> response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED, "Autenticación requerida")))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}