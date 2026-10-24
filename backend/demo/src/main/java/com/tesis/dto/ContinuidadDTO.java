package com.tesis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public final class ContinuidadDTO {

    private ContinuidadDTO() {
    }

    public record GeneracionResponseDTO(
            Short anioOrigen,
            Short anioDestino,
            int continuidadesGeneradas,
            List<ContinuidadGeneradaDTO> continuidades,
            List<AsignacionPendienteDTO> asignacionPendiente,
            List<EgresoPendienteResponseDTO> egresosPendientes) {
    }

    public record ContinuidadGeneradaDTO(
            Integer matriculaId,
            Integer estudianteId,
            String estudianteCedula,
            String estudianteNombreCompleto,
            Integer gradoOrigenId,
            Short numeroGradoOrigen,
            Integer gradoDestinoId,
            Short numeroGradoDestino,
            Integer seccionId,
            String letraSeccion) {
    }

    public record AsignacionPendienteDTO(
            Integer estudianteId,
            String estudianteCedula,
            String estudianteNombreCompleto,
            Integer gradoDestinoId,
            Short numeroGradoDestino,
            String motivo) {
    }

    public record ResolverEgresoRequestDTO(
            @NotBlank
            @Pattern(regexp = "egresar|repetir",
                    message = "La decisión debe ser 'egresar' o 'repetir'")
            String decision,
            Integer seccionId) {
    }

    public record EgresoPendienteResponseDTO(
            Integer egresoId,
            Integer matriculaOrigenId,
            Integer estudianteId,
            String estudianteCedula,
            String estudianteNombreCompleto,
            Short anioOrigen,
            Short numeroGradoOrigen,
            String resolucion,
            Integer matriculaDestinoId,
            Integer resueltoPorId,
            LocalDateTime resueltoEn) {
    }
}
