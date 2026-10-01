package com.tesis.dto;

import com.tesis.entity.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class UsuarioDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioRequestDTO {
        private String nombreUsuario;
        private String email;
        private String password;
        @Deprecated
        private String passwordHash;
        private Integer rolId;
        @Builder.Default
        private TipoUsuario tipoUsuario = TipoUsuario.PERSONAL;
        @Builder.Default
        private Boolean activo = true;

        public String getPassword() {
            return password != null ? password : passwordHash;
        }

        public void setPassword(String password) {
            this.password = password;
            if (this.passwordHash == null) {
                this.passwordHash = password;
            }
        }

        public String getPasswordHash() {
            return passwordHash != null ? passwordHash : password;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            if (this.password == null) {
                this.password = passwordHash;
            }
        }
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