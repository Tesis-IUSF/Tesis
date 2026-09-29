package com.tesis.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                String token = authorization.substring(BEARER_PREFIX.length());
                Claims claims = jwtProvider.validarToken(token);
                String rol = normalizarRol(claims.get("rolNombre", String.class));
                if (rol == null || claims.getSubject() == null || claims.getSubject().isBlank()) {
                    throw new IllegalArgumentException("El token no contiene identidad y rol");
                }
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(rol));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String normalizarRol(String rol) {
        if (rol == null || rol.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(rol, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}