package com.tesis.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {

    @Value("${app.jwt.secret:tu_clave_secreta_super_segura_minimo_32_caracteres}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration; // 24 horas en milisegundos

    /**
     * Genera un token JWT para un usuario
     */
    public String generarToken(Integer usuarioId, String email, String nombreUsuario) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            return Jwts.builder()
                    .subject(email)
                    .claim("usuarioId", usuarioId)
                    .claim("nombreUsuario", nombreUsuario)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(key, SignatureAlgorithm.HS256)
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
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
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
}