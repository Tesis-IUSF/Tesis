package com.tesis.service;

import com.tesis.dto.EstadisticasMatriculaDTO;
import com.tesis.dto.EstadisticasMatriculaDTO.EstadisticaSeccionDTO;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class EstadisticasPdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.institucion.nombre:Sistema Educativo}")
    private String institucion;

    public byte[] generar(EstadisticasMatriculaDTO estadisticas) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 54, 54, 48, 48);
            PdfWriter.getInstance(document, output);
            document.open();

            Paragraph encabezado = new Paragraph(institucion,
                    new Font(Font.HELVETICA, 17, Font.BOLD, new Color(28, 79, 72)));
            encabezado.setAlignment(Element.ALIGN_CENTER);
            document.add(encabezado);

            Paragraph titulo = new Paragraph("REPORTE DE INSCRIPCIÓN AL " + LocalDate.now().format(FORMATO_FECHA),
                    new Font(Font.HELVETICA, 14, Font.BOLD));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(24);
            titulo.setSpacingAfter(18);
            document.add(titulo);

            PdfPTable resumen = new PdfPTable(new float[] {2.2f, 1.0f});
            resumen.setWidthPercentage(100);
            agregarFila(resumen, "Cupos disponibles", estadisticas.getCuposDisponibles());
            agregarFila(resumen, "Cupos ocupados", estadisticas.getCuposOcupados());
            agregarFila(resumen, "Ocupación", estadisticas.getPorcentajeOcupacion() + "%");
            agregarFila(resumen, "Nuevo ingreso", estadisticas.getNuevoIngreso());
            agregarFila(resumen, "Regulares pendientes", estadisticas.getRegularesPendientes());
            document.add(resumen);

            agregarDesglosePorSeccion(document, estadisticas);

            Paragraph fecha = new Paragraph("Generado el " + LocalDate.now().format(FORMATO_FECHA),
                    new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY));
            fecha.setAlignment(Element.ALIGN_RIGHT);
            fecha.setSpacingBefore(20);
            document.add(fecha);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el reporte de estadísticas PDF", exception);
        }
    }

    private void agregarDesglosePorSeccion(Document document, EstadisticasMatriculaDTO estadisticas)
            throws Exception {
        List<String> sexos = obtenerSexos(estadisticas);
        String nivelActual = null;
        PdfPTable tablaNivel = null;
        Map<String, Long> totalesSexoNivel = new LinkedHashMap<>();
        long totalNivel = 0;
        Map<String, Long> totalesSexo = new LinkedHashMap<>();
        long matriculaTotal = 0;

        for (EstadisticaSeccionDTO seccion : estadisticas.getEstudiantesPorSeccion()) {
            if (!Objects.equals(nivelActual, seccion.getNivel())) {
                if (tablaNivel != null) {
                    agregarTotalNivel(tablaNivel, sexos, totalesSexoNivel, totalNivel);
                    document.add(tablaNivel);
                }
                nivelActual = seccion.getNivel();
                totalesSexoNivel = new LinkedHashMap<>();
                totalNivel = 0;

                Paragraph tituloNivel = new Paragraph(nivelActual.toUpperCase(),
                        new Font(Font.HELVETICA, 12, Font.BOLD));
                tituloNivel.setSpacingBefore(20);
                tituloNivel.setSpacingAfter(8);
                document.add(tituloNivel);

                float[] anchos = new float[sexos.size() + 2];
                anchos[0] = 2.2f;
                for (int indice = 1; indice <= sexos.size(); indice++) {
                    anchos[indice] = 0.7f;
                }
                anchos[anchos.length - 1] = 0.9f;
                tablaNivel = new PdfPTable(anchos);
                tablaNivel.setWidthPercentage(100);
                agregarCelda(tablaNivel, "Sección", true);
                for (String sexo : sexos) {
                    agregarCelda(tablaNivel, sexo, true);
                }
                agregarCelda(tablaNivel, "Total", true);
            }

            agregarCelda(tablaNivel, seccion.getSeccion(), false);
            for (String sexo : sexos) {
                agregarCelda(tablaNivel,
                        Long.toString(seccion.getEstudiantesPorSexo().getOrDefault(sexo, 0L)), false);
            }
            agregarCelda(tablaNivel, Long.toString(seccion.getTotal()), false);
            sumarTotales(totalesSexoNivel, seccion.getEstudiantesPorSexo());
            totalNivel += seccion.getTotal();
            sumarTotales(totalesSexo, seccion.getEstudiantesPorSexo());
            matriculaTotal += seccion.getTotal();
        }

        if (tablaNivel != null) {
            agregarTotalNivel(tablaNivel, sexos, totalesSexoNivel, totalNivel);
            document.add(tablaNivel);
        }

        StringBuilder resumenTotal = new StringBuilder("Total matrícula inscrita:");
        for (String sexo : sexos) {
            resumenTotal.append(" ").append(sexo).append("=").append(totalesSexo.getOrDefault(sexo, 0L));
        }
        resumenTotal.append(" Total=").append(matriculaTotal);
        Paragraph totalGeneral = new Paragraph(resumenTotal.toString(),
                new Font(Font.HELVETICA, 11, Font.BOLD));
        totalGeneral.setSpacingBefore(16);
        document.add(totalGeneral);
    }

    private List<String> obtenerSexos(EstadisticasMatriculaDTO estadisticas) {
        LinkedHashSet<String> sexos = new LinkedHashSet<>();
        for (EstadisticaSeccionDTO seccion : estadisticas.getEstudiantesPorSeccion()) {
            sexos.addAll(seccion.getEstudiantesPorSexo().keySet());
        }
        return new ArrayList<>(sexos);
    }

    private void sumarTotales(Map<String, Long> totales, Map<String, Long> cantidades) {
        cantidades.forEach((sexo, cantidad) -> totales.merge(sexo, cantidad, Long::sum));
    }

    private void agregarTotalNivel(PdfPTable table,
                                  List<String> sexos,
                                  Map<String, Long> totalesSexo,
                                  long total) {
        agregarCelda(table, "Total nivel", true);
        for (String sexo : sexos) {
            agregarCelda(table, Long.toString(totalesSexo.getOrDefault(sexo, 0L)), true);
        }
        agregarCelda(table, Long.toString(total), true);
    }

    private void agregarFila(PdfPTable table, String etiqueta, Object valor) {
        agregarCelda(table, etiqueta, false);
        agregarCelda(table, String.valueOf(valor), false);
    }

    private void agregarCelda(PdfPTable table, String texto, boolean encabezado) {
        PdfPCell celda = new PdfPCell(new Phrase(texto,
                new Font(Font.HELVETICA, 10, encabezado ? Font.BOLD : Font.NORMAL)));
        celda.setPadding(8);
        celda.setBorderColor(new Color(210, 218, 215));
        if (encabezado) {
            celda.setBackgroundColor(new Color(238, 243, 241));
        }
        table.addCell(celda);
    }
}