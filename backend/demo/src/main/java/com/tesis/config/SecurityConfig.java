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
            .requestMatchers("/api/auth/login", "/api/auth/registrar").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/asistencias/qr").hasRole("ESCANER")
            .requestMatchers(HttpMethod.POST, "/api/empleados/*/carnet")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.GET, "/api/empleados/carnets")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers(HttpMethod.POST, "/api/empleados/carnets/lote")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR")
            .requestMatchers("/api/matriculas/**")
                .hasAnyRole("ADMINISTRADOR", "DIRECTOR", "ADMINISTRATIVO")
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