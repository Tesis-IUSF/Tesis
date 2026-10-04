package com.tesis.service;

import com.tesis.dto.EstadisticasMatriculaDTO;
import com.tesis.dto.EstadisticasMatriculaDTO.EstadisticaSeccionDTO;
import com.tesis.entity.NivelEducativo;
import com.tesis.repository.EstadisticasRepository;
import com.tesis.repository.NivelEducativoRepository;
import com.tesis.repository.SeccionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class EstadisticasMatriculaService {

    private final EstadisticasRepository estadisticasRepository;
    private final SeccionRepository seccionRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final EstadisticasPdfGenerator estadisticasPdfGenerator;

    public EstadisticasMatriculaService(EstadisticasRepository estadisticasRepository,
                                        SeccionRepository seccionRepository,
                                        NivelEducativoRepository nivelEducativoRepository,
                                        EstadisticasPdfGenerator estadisticasPdfGenerator) {
        this.estadisticasRepository = estadisticasRepository;
        this.seccionRepository = seccionRepository;
        this.nivelEducativoRepository = nivelEducativoRepository;
        this.estadisticasPdfGenerator = estadisticasPdfGenerator;
    }

    @Cacheable(cacheNames = "estadisticasMatricula", key = "'general'")
    public EstadisticasMatriculaDTO obtenerEstadisticasGenerales() {
        return calcularEstadisticas(null, null);
    }

    public byte[] exportarEstadisticasGeneralesPDF() {
        return estadisticasPdfGenerator.generar(obtenerEstadisticasGenerales());
    }

    @Cacheable(cacheNames = "estadisticasMatricula", key = "'seccion:' + #seccionId")
    public EstadisticasMatriculaDTO obtenerEstadisticasSeccion(Integer seccionId) {
        if (!seccionRepository.existsById(seccionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada");
        }
        return calcularEstadisticas(seccionId, null);
    }

    @Cacheable(cacheNames = "estadisticasMatricula", key = "'nivel:' + #nivel.toLowerCase()")
    public EstadisticasMatriculaDTO obtenerEstadisticasNivel(String nivel) {
        NivelEducativo nivelEducativo = nivelEducativoRepository.findByNombreIgnoreCase(nivel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nivel educativo no encontrado"));
        return calcularEstadisticas(null, nivelEducativo.getNombre());
    }

    private EstadisticasMatriculaDTO calcularEstadisticas(Integer seccionId, String nivel) {
        long cuposDisponibles = valor(estadisticasRepository.obtenerCuposDisponibles(seccionId, nivel));
        long cuposOcupados = valor(estadisticasRepository.obtenerCuposOcupados(seccionId, nivel));
        long capacidadTotal = valor(estadisticasRepository.obtenerCapacidadTotal(seccionId, nivel));
        double porcentajeOcupacion = capacidadTotal == 0
                ? 0.0 : Math.round(cuposOcupados * 10000.0 / capacidadTotal) / 100.0;

        Map<String, Long> estudiantesPorSexo = new LinkedHashMap<>();
        for (Object[] fila : estadisticasRepository.obtenerEstudiantesPorSexo(seccionId, nivel)) {
            estudiantesPorSexo.put((String) fila[0], ((Number) fila[1]).longValue());
        }

        List<EstadisticaSeccionDTO> estudiantesPorSeccion = obtenerEstadisticasPorSeccion(seccionId, nivel);

        return new EstadisticasMatriculaDTO(
                cuposDisponibles,
                cuposOcupados,
                porcentajeOcupacion,
                estudiantesPorSexo,
                estudiantesPorSeccion,
                valor(estadisticasRepository.obtenerNuevoIngreso(seccionId, nivel)),
                valor(estadisticasRepository.obtenerRegularesPendientes(seccionId, nivel)));
    }

    private List<EstadisticaSeccionDTO> obtenerEstadisticasPorSeccion(Integer seccionId, String nivel) {
        Map<Integer, EstadisticaSeccionDTO> porSeccion = new LinkedHashMap<>();
        for (Object[] fila : estadisticasRepository.obtenerEstudiantesPorSeccionYSexo(seccionId, nivel)) {
            Integer seccionIdFila = ((Number) fila[5]).intValue();
            EstadisticaSeccionDTO estadistica = porSeccion.computeIfAbsent(seccionIdFila,
                    ignorado -> new EstadisticaSeccionDTO((String) fila[0],
                            nombreSeccion((String) fila[0], (Number) fila[2], (String) fila[3],
                                    (String) fila[4]), new LinkedHashMap<>(), 0));
            long cantidad = ((Number) fila[7]).longValue();
            String sexo = (String) fila[6];
            estadistica.getEstudiantesPorSexo().merge(sexo, cantidad, Long::sum);
            estadistica.setTotal(estadistica.getTotal() + cantidad);
        }
        return List.copyOf(porSeccion.values());
    }

    private String nombreSeccion(String nivel, Number numeroGrado, String nombreEspecial, String letra) {
        if (nombreEspecial != null && !nombreEspecial.isBlank()) {
            return nombreEspecial;
        }
        int numero = numeroGrado.intValue();
        String sufijo = switch (numero) {
            case 1, 3 -> "er";
            case 2 -> "do";
            default -> "to";
        };
        String unidad = nivel.toLowerCase(Locale.ROOT).contains("media") ? "año" : "Grado";
        String base = numero + sufijo + " " + unidad;
        return letra == null || letra.isBlank() ? base : base + " " + letra;
    }

    private long valor(Long valor) {
        return valor == null ? 0L : valor;
    }
}