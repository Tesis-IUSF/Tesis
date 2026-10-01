package com.tesis.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CarnetDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpleadoCarnetResponseDTO {
        private Integer empleadoId;
        private String nombreCompleto;
        private String cedula;
        private String cargo;
        private String departamento;
        private String estadoQr;
        private LocalDateTime expiraEn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneracionLoteRequestDTO {
        @NotEmpty
        @Size(max = 100)
        private List<Integer> empleadoIds;

        @NotBlank
        @Pattern(regexp = "(?i)zip|pdf")
        private String formato;
    }
}