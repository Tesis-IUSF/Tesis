package com.tesis.dto;

import com.tesis.entity.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UsuarioDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioRequestDTO {
        private String nombreUsuario;
        private String email;
        private String passwordHash;
        private Integer rolId;
        private TipoUsuario tipoUsuario = TipoUsuario.PERSONAL;
        private Boolean activo = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioResponseDTO {
        private Integer id;
        private String nombreUsuario;
        private String email;
        private Integer rolId;
        private String rolNombre; // Nombre del rol
        private TipoUsuario tipoUsuario;
        private Boolean activo;
        private Boolean emailVerificado;
        private LocalDateTime ultimoAcceso;
        private LocalDateTime bloqueadoHasta;
        private LocalDateTime creadoEn;
        private LocalDateTime actualizadoEn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioSimpledDTO {
        private Integer id;
        private String nombreUsuario;
        private String email;
        private TipoUsuario tipoUsuario;
        private Boolean activo;
    }
}