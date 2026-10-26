package com.tesis.service;

import com.tesis.dto.AsistenciaDTO.AsistenciaResponseDTO;
import com.tesis.dto.AsistenciaDTO.AusenciaDTO;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AsistenciaPdfGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.institucion.nombre:C.E.N. Lcda. Roraima Asansa Escandela}")
    private String institucion;

    @Value("${app.institucion.municipio:San Francisco}")
    private String municipio;

    @Value("${app.institucion.estado:Zulia}")
    private String estado;

    @Value("${app.institucion.codigo-plantel:OD34942317}")
    private String codigoPlantel;

    public byte[] generar(LocalDate desde,
                          LocalDate hasta,
                          List<AsistenciaResponseDTO> registros,
                          List<AusenciaDTO> ausencias,
                          boolean incluirAusencias) {
        try {
            List<FilaReporte> filas = combinar(registros, ausencias, incluirAusencias);
            long diasTrabajados = filas.stream().filter(fila -> fila.horaEntrada() != null).count();
            long inasistencias = filas.stream().filter(fila -> "ausente".equalsIgnoreCase(fila.estado())).count();
            long tardanzas = filas.stream().filter(fila -> "tardanza".equalsIgnoreCase(fila.estado())).count();
            long minutosTrabajados = filas.stream()
                    .mapToLong(fila -> minutosTrabajados(fila.horaEntrada(), fila.horaSalida()))
                    .sum();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(MembretePdf.crear(institucion, municipio, estado, codigoPlantel));

            Paragraph titulo = new Paragraph("REPORTE HISTÓRICO DE ASISTENCIA",
                    new Font(Font.HELVETICA, 13, Font.BOLD));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(8);
            titulo.setSpacingAfter(4);
            document.add(titulo);

            Paragraph periodo = new Paragraph("Período: " + fecha(desde) + " al " + fecha(hasta),
                    new Font(Font.HELVETICA, 9));
            periodo.setAlignment(Element.ALIGN_CENTER);
            periodo.setSpacingAfter(12);
            document.add(periodo);

            PdfPTable resumen = new PdfPTable(new float[] {1, 0.7f, 1, 0.7f, 1, 0.7f, 1.2f, 0.8f});
            resumen.setWidthPercentage(100);
            agregarCelda(resumen, "Asistencias", true);
            agregarCelda(resumen, Long.toString(diasTrabajados), false);
            agregarCelda(resumen, "Inasistencias", true);
            agregarCelda(resumen, Long.toString(inasistencias), false);
            agregarCelda(resumen, "Tardanzas", true);
            agregarCelda(resumen, Long.toString(tardanzas), false);
            agregarCelda(resumen, "Horas trabajadas", true);
            agregarCelda(resumen, formatoDuracion(minutosTrabajados), false);
            document.add(resumen);

            Paragraph tituloDetalle = new Paragraph("Detalle por registro",
                    new Font(Font.HELVETICA, 11, Font.BOLD));
            tituloDetalle.setSpacingBefore(16);
            tituloDetalle.setSpacingAfter(7);
            document.add(tituloDetalle);

            PdfPTable tabla = new PdfPTable(new float[] {2.1f, 1.2f, 1.0f, 0.85f, 0.85f,
                    1.1f, 0.9f, 0.9f, 1.2f, 1.2f});
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            tabla.setSplitLate(false);
            for (String columna : List.of("Empleado", "Cédula", "Fecha", "Entrada", "Salida",
                    "Estado", "Min. tarde", "Min. extra", "Turno", "Tipo de registro")) {
                agregarCelda(tabla, columna, true);
            }

            for (FilaReporte fila : filas) {
                agregarCelda(tabla, texto(fila.empleado()), false);
                agregarCelda(tabla, texto(fila.cedula()), false);
                agregarCelda(tabla, fecha(fila.fecha()), false);
                agregarCelda(tabla, hora(fila.horaEntrada()), false);
                agregarCelda(tabla, hora(fila.horaSalida()), false);
                agregarCelda(tabla, texto(fila.estado()), false);
                agregarCelda(tabla, fila.minutosTardanza() == null
                        ? "—" : fila.minutosTardanza().toString(), false);
                agregarCelda(tabla, fila.minutosExtra() == null
                        ? "—" : fila.minutosExtra().toString(), false);
                agregarCelda(tabla, texto(fila.turno()), false);
                agregarCelda(tabla, fila.tipoRegistro(), false);
            }
            document.add(tabla);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el reporte de asistencia PDF", exception);
        }
    }

    private List<FilaReporte> combinar(List<AsistenciaResponseDTO> registros,
                                       List<AusenciaDTO> ausencias,
                                       boolean incluirAusencias) {
        List<FilaReporte> filas = new ArrayList<>();
        registros.forEach(registro -> filas.add(new FilaReporte(
                registro.getEmpleadoNombre(),
                registro.getEmpleadoCedula(),
                registro.getFecha(),
                registro.getHoraEntrada(),
                registro.getHoraSalida(),
                registro.getEstado(),
                registro.getMinutosTardanza(),
                registro.getMinutosExtra(),
                registro.getTurnoNombre(),
                tipoRegistro(registro.getHoraEntrada(), registro.getHoraSalida()))));
        if (incluirAusencias) {
            ausencias.forEach(ausencia -> filas.add(new FilaReporte(
                    (ausencia.getNombre() + " " + ausencia.getApellido()).trim(),
                    ausencia.getCedula(),
                    ausencia.getFecha(),
                    null,
                    null,
                    "ausente",
                    null,
                    null,
                    ausencia.getTurnoNombre(),
                    "Sin marcaje")));
        }
        filas.sort(Comparator.comparing(FilaReporte::fecha).reversed()
                .thenComparing(FilaReporte::empleado, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    private String tipoRegistro(LocalTime entrada, LocalTime salida) {
        if (entrada != null && salida != null) {
            return "Entrada y salida";
        }
        if (entrada != null) {
            return "Solo entrada";
        }
        if (salida != null) {
            return "Solo salida";
        }
        return "Sin marcaje";
    }

    private long minutosTrabajados(LocalTime entrada, LocalTime salida) {
        if (entrada == null || salida == null) {
            return 0;
        }
        long minutos = Duration.between(entrada, salida).toMinutes();
        return minutos < 0 ? minutos + Duration.ofDays(1).toMinutes() : minutos;
    }

    private String formatoDuracion(long minutos) {
        return (minutos / 60) + ":" + String.format("%02d", minutos % 60);
    }

    private void agregarCelda(PdfPTable tabla, String texto, boolean encabezado) {
        PdfPCell celda = new PdfPCell(new Phrase(texto,
                new Font(Font.HELVETICA, encabezado ? 8 : 7, encabezado ? Font.BOLD : Font.NORMAL)));
        celda.setPadding(5);
        celda.setBorderColor(new Color(210, 218, 215));
        if (encabezado) {
            celda.setBackgroundColor(new Color(238, 243, 241));
        }
        tabla.addCell(celda);
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? "—" : FECHA.format(fecha);
    }

    private String hora(LocalTime hora) {
        return hora == null ? "—" : hora.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String texto(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private record FilaReporte(String empleado,
                               String cedula,
                               LocalDate fecha,
                               LocalTime horaEntrada,
                               LocalTime horaSalida,
                               String estado,
                               Short minutosTardanza,
                               Short minutosExtra,
                               String turno,
                               String tipoRegistro) {
    }
}
