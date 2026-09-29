package com.tesis.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration; // 24 horas en milisegundos

    @PostConstruct
    void validateConfiguration() {
        signingKey();
    }

    /**
     * Genera un token JWT para un usuario
     */
    public String generarToken(Integer usuarioId, String email, String nombreUsuario, String rolNombre) {
        try {
            return Jwts.builder()
                    .subject(email)
                    .claim("usuarioId", usuarioId)
                    .claim("nombreUsuario", nombreUsuario)
                    .claim("rolNombre", rolNombre)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(signingKey())
                    .compact();
        } catch (Exception e) {
            log.error("Error generando token JWT", e);
            throw new RuntimeException("Error al generar token", e);
        }
    }

    /**
     * Valida un token JWT y retorna las claims
     */
    public Claims validarToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error validando token JWT: {}", e.getMessage());
            throw new RuntimeException("Token inválido o expirado", e);
        }
    }

    /**
     * Obtiene el email del token
     */
    public String obtenerEmailDelToken(String token) {
        return validarToken(token).getSubject();
    }

    /**
     * Obtiene el ID del usuario del token
     */
    public Integer obtenerUsuarioIdDelToken(String token) {
        return validarToken(token).get("usuarioId", Integer.class);
    }

    /**
     * Verifica si el token es válido
     */
    public boolean esTokenValido(String token) {
        try {
            validarToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}