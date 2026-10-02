package com.tesis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class BlogDTO {

    private BlogDTO() {
    }

    public record CategoriaRequestDTO(
            @NotBlank @Size(max = 100) String nombre,
            String descripcion,
            @Size(max = 500) String iconoUrl,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorHex,
            Short ordinal,
            Boolean activo) {
    }

    public record CategoriaResponseDTO(
            Integer id,
            String nombre,
            String descripcion,
            String iconoUrl,
            String colorHex,
            Short ordinal,
            Boolean activo,
            LocalDateTime creadoEn) {
    }

    public record ComunicadoRequestDTO(
            @NotBlank @Size(max = 255) String titulo,
            @NotBlank String contenidoHtml,
            @Size(max = 500) String resumen,
            @NotNull Integer categoriaId,
            Boolean publicado,
            LocalDateTime fechaPublicacion,
            LocalDateTime fechaExpiracion,
            @Size(max = 500) String imagenUrl,
            Integer ordenVisualizacion,
            Boolean visiblePublico) {
    }

    public record ComunicadoResponseDTO(
            Integer id,
            String titulo,
            String contenidoHtml,
            String resumen,
            Integer categoriaId,
            String categoriaNombre,
            Integer creadoPorId,
            Boolean publicado,
            LocalDateTime fechaPublicacion,
            LocalDateTime fechaExpiracion,
            String imagenUrl,
            Integer ordenVisualizacion,
            Boolean visiblePublico,
            LocalDateTime creadoEn,
            LocalDateTime actualizadoEn) {
    }
}