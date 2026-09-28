package com.tesis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RolDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RolRequestDTO {
        private String nombreRol;
        private String descripcion;
        private Boolean activo = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RolResponseDTO {
        private Integer id;
        private String nombreRol;
        private String descripcion;
        private Boolean activo;
    }
}