package com.tesis.service;

import com.tesis.entity.Empleado;
import com.tesis.entity.CredencialQr;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.EmpleadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final CredencialQrRepository credencialQrRepository;

    public CarnetLoteService(CarnetService carnetService,
                             CarnetPdfGenerator pdfGenerator,
                             EmpleadoRepository empleadoRepository,
                             CredencialQrRepository credencialQrRepository) {
        this.carnetService = carnetService;
        this.pdfGenerator = pdfGenerator;
        this.empleadoRepository = empleadoRepository;
        this.credencialQrRepository = credencialQrRepository;
    }

    @Transactional
    public byte[] generar(List<Integer> idsSolicitados, String formatoSolicitado) {
        SolicitudLote solicitud = validarSolicitud(idsSolicitados, formatoSolicitado);
        LinkedHashSet<Integer> ids = solicitud.ids();
        String formato = solicitud.formato();
        Map<Integer, Empleado> empleados = new HashMap<>();
        empleadoRepository.findAllById(ids).forEach(empleado -> empleados.put(empleado.getId(), empleado));
        validarEmpleados(ids, empleados);

        List<byte[]> carnets = new ArrayList<>();
        for (Integer id : ids) {
            carnets.add(carnetService.generarCarnet(id));
        }
        return empaquetar(ids, formato, empleados, carnets);
    }

    @Transactional(readOnly = true)
    public byte[] descargarExistentes(List<Integer> idsSolicitados, String formatoSolicitado) {
        SolicitudLote solicitud = validarSolicitud(idsSolicitados, formatoSolicitado);
        LinkedHashSet<Integer> ids = solicitud.ids();
        String formato = solicitud.formato();

        Map<Integer, Empleado> empleados = new HashMap<>();
        empleadoRepository.findAllById(ids).forEach(empleado -> empleados.put(empleado.getId(), empleado));
        validarEmpleados(ids, empleados);

        LocalDateTime ahora = LocalDateTime.now();
        Map<Integer, CredencialQr> credenciales = new HashMap<>();
        List<String> sinCredencialVigente = new ArrayList<>();
        for (Integer id : ids) {
            CredencialQr credencial = credencialQrRepository.findByEmpleado_IdAndActivaTrue(id).stream()
                    .filter(candidata -> candidata.getExpiraEn().isAfter(ahora))
                    .max(Comparator.comparing(CredencialQr::getCreadaEn))
                    .orElse(null);
            if (credencial == null) {
                Empleado empleado = empleados.get(id);
                sinCredencialVigente.add(empleado.getNombre() + " " + empleado.getApellido()
                        + " (ID: " + id + ")");
            } else {
                credenciales.put(id, credencial);
            }
        }
        if (!sinCredencialVigente.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay credencial activa y vigente para: " + String.join(", ", sinCredencialVigente));
        }

        List<byte[]> carnets = new ArrayList<>();
        for (Integer id : ids) {
            carnets.add(carnetService.generarCarnetExistente(empleados.get(id), credenciales.get(id)));
        }
        return empaquetar(ids, formato, empleados, carnets);
    }

    private SolicitudLote validarSolicitud(List<Integer> idsSolicitados, String formatoSolicitado) {
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
        return new SolicitudLote(ids, formato);
    }

    private void validarEmpleados(LinkedHashSet<Integer> ids, Map<Integer, Empleado> empleados) {
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
    }

    private byte[] empaquetar(LinkedHashSet<Integer> ids,
                              String formato,
                              Map<Integer, Empleado> empleados,
                              List<byte[]> carnets) {
        if (formato.equals("pdf")) {
            return pdfGenerator.unirCarnets(carnets);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringBuilder manifiesto = new StringBuilder("empleado_id,nombre,apellido,ci,resultado\n");
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                int indice = 0;
                for (Integer id : ids) {
                    Empleado empleado = empleados.get(id);
                    byte[] pdf = carnets.get(indice++);
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

    private record SolicitudLote(LinkedHashSet<Integer> ids, String formato) {
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