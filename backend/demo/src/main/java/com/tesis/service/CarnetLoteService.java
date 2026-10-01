package com.tesis.service;

import com.tesis.entity.Empleado;
import com.tesis.repository.EmpleadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CarnetLoteService {

    private static final int MAX_EMPLEADOS_POR_LOTE = 100;
    private final CarnetService carnetService;
    private final CarnetPdfGenerator pdfGenerator;
    private final EmpleadoRepository empleadoRepository;

    public CarnetLoteService(CarnetService carnetService,
                             CarnetPdfGenerator pdfGenerator,
                             EmpleadoRepository empleadoRepository) {
        this.carnetService = carnetService;
        this.pdfGenerator = pdfGenerator;
        this.empleadoRepository = empleadoRepository;
    }

    @Transactional
    public byte[] generar(List<Integer> idsSolicitados, String formatoSolicitado) {
        if (idsSolicitados == null || idsSolicitados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seleccione al menos un empleado");
        }
        LinkedHashSet<Integer> ids = new LinkedHashSet<>(idsSolicitados);
        if (ids.contains(null) || ids.size() > MAX_EMPLEADOS_POR_LOTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La tanda debe contener entre 1 y " + MAX_EMPLEADOS_POR_LOTE + " empleados distintos");
        }

        String formato = formatoSolicitado == null ? "" : formatoSolicitado.trim().toLowerCase(Locale.ROOT);
        if (!formato.equals("zip") && !formato.equals("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El formato debe ser zip o pdf");
        }

        Map<Integer, Empleado> empleados = new HashMap<>();
        empleadoRepository.findAllById(ids).forEach(empleado -> empleados.put(empleado.getId(), empleado));
        for (Integer id : ids) {
            Empleado empleado = empleados.get(id);
            if (empleado == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Empleado no encontrado: " + id);
            }
            if (!Boolean.TRUE.equals(empleado.getActivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede emitir carnet para un empleado inactivo: " + id);
            }
        }

        if (formato.equals("pdf")) {
            List<byte[]> carnets = new ArrayList<>();
            for (Integer id : ids) {
                carnets.add(carnetService.generarCarnet(id));
            }
            return pdfGenerator.unirCarnets(carnets);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringBuilder manifiesto = new StringBuilder("empleado_id,nombre,apellido,ci,resultado\n");
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                for (Integer id : ids) {
                    Empleado empleado = empleados.get(id);
                    byte[] pdf = carnetService.generarCarnet(id);
                    zip.putNextEntry(new ZipEntry(nombreArchivo(empleado)));
                    zip.write(pdf);
                    zip.closeEntry();
                    manifiesto.append(id).append(',').append(csv(empleado.getNombre())).append(',')
                            .append(csv(empleado.getApellido())).append(',').append(csv(empleado.getCedula()))
                            .append(",generado\n");
                }
                zip.putNextEntry(new ZipEntry("resultado.csv"));
                zip.write(manifiesto.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo preparar el archivo ZIP de carnets", exception);
        }
    }

    private String nombreArchivo(Empleado empleado) {
        return "carnet-" + slug(empleado.getNombre()) + "-" + slug(empleado.getApellido())
                + "-" + slug(empleado.getCedula()) + ".pdf";
    }

    private String slug(String valor) {
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String csv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}