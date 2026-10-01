package com.tesis.service;

import com.tesis.entity.Matricula;
import com.tesis.entity.RetiroMatricula;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ConstanciaPdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.institucion.nombre:Sistema Educativo}")
    private String institucion;

    public byte[] generarInscripcion(Matricula matricula) {
        return generar("CONSTANCIA DE INSCRIPCIÓN", document -> {
            document.add(new Paragraph("Por medio de la presente se hace constar que:", font(12, Font.NORMAL)));
            document.add(new Paragraph(matricula.getEstudiante().getNombre() + " "
                    + matricula.getEstudiante().getApellido(), font(16, Font.BOLD)));
            document.add(new Paragraph("se encuentra inscrito(a) en esta institución para el año escolar "
                    + periodoEscolar(matricula.getAnioEscolar()) + ".", font(12, Font.NORMAL)));

            PdfPTable details = tablaDetalles();
            agregarDetalle(details, "N.º de matrícula", matricula.getId().toString());
            agregarDetalle(details, "N.º de constancia", texto(matricula.getNumeroConstancia(), "No asignado"));
            agregarDetalle(details, "Cédula del estudiante", matricula.getEstudiante().getCedula());
            agregarDetalle(details, "Nivel educativo", matricula.getSeccion().getGrado()
                    .getNivelEducativo().getNombre());
            agregarDetalle(details, "Grado", grado(matricula));
            agregarDetalle(details, "Sección", matricula.getSeccion().getLetraSeccion());
            agregarDetalle(details, "Fecha de formalización", fecha(matricula.getFechaFormalizacion()));
            document.add(details);
        });
    }

        public byte[] generarAsignacionCupo(Matricula matricula) {
            return generar("CONSTANCIA DE ACEPTACIÓN DE CUPO", document -> {
            document.add(new Paragraph("Por medio de la presente se hace constar que:",
                font(12, Font.NORMAL)));
            document.add(new Paragraph(matricula.getEstudiante().getNombre() + " "
                + matricula.getEstudiante().getApellido(), font(16, Font.BOLD)));
            document.add(new Paragraph("titular de la cédula de identidad "
                + matricula.getEstudiante().getCedula() + ", tiene un cupo reservado en "
                    + institucion + " para el período escolar "
                    + periodoEscolar(matricula.getAnioEscolar()) + ".",
                font(12, Font.NORMAL)));

            PdfPTable details = tablaDetalles();
            agregarDetalle(details, "N.º de matrícula", matricula.getId().toString());
            agregarDetalle(details, "Nivel", matricula.getSeccion().getGrado()
                .getNivelEducativo().getNombre());
            agregarDetalle(details, "Grado", grado(matricula));
            agregarDetalle(details, "Sección", matricula.getSeccion().getLetraSeccion());
            agregarDetalle(details, "Período escolar", periodoEscolar(matricula.getAnioEscolar()));
            agregarDetalle(details, "Fecha de preinscripción", fecha(matricula.getFechaSolicitud()));
            document.add(details);
        });
        }

    public byte[] generarRetiro(RetiroMatricula retiro) {
        Matricula matricula = retiro.getMatricula();
        return generar("CONSTANCIA DE RETIRO", document -> {
            document.add(new Paragraph("Por medio de la presente se hace constar el retiro del(de la) estudiante:",
                    font(12, Font.NORMAL)));
            document.add(new Paragraph(matricula.getEstudiante().getNombre() + " "
                    + matricula.getEstudiante().getApellido(), font(16, Font.BOLD)));
                document.add(new Paragraph("correspondiente al año escolar "
                    + periodoEscolar(matricula.getAnioEscolar()) + ".",
                    font(12, Font.NORMAL)));

            PdfPTable details = tablaDetalles();
            agregarDetalle(details, "N.º de retiro", retiro.getId().toString());
            agregarDetalle(details, "N.º de matrícula", matricula.getId().toString());
            agregarDetalle(details, "Cédula del estudiante", matricula.getEstudiante().getCedula());
            agregarDetalle(details, "Nivel educativo", matricula.getSeccion().getGrado()
                    .getNivelEducativo().getNombre());
            agregarDetalle(details, "Grado", grado(matricula));
            agregarDetalle(details, "Sección", matricula.getSeccion().getLetraSeccion());
            agregarDetalle(details, "Fecha de retiro", fecha(retiro.getFechaRetiro()));
            agregarDetalle(details, "Solicitante", retiro.getSolicitanteNombre() + " "
                    + retiro.getSolicitanteApellido() + " - " + retiro.getSolicitanteCedula());
            agregarDetalle(details, "Motivo", retiro.getMotivo());
            document.add(details);
        });
    }

    private byte[] generar(String titulo, ContenidoDocumento contenido) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 58, 58, 54, 54);
            PdfWriter.getInstance(document, output);
            document.open();

            Paragraph encabezado = new Paragraph(institucion, font(17, Font.BOLD, new Color(28, 79, 72)));
            encabezado.setAlignment(Element.ALIGN_CENTER);
            document.add(encabezado);
            Paragraph nombreDocumento = new Paragraph(titulo, font(14, Font.BOLD));
            nombreDocumento.setAlignment(Element.ALIGN_CENTER);
            nombreDocumento.setSpacingBefore(30);
            nombreDocumento.setSpacingAfter(24);
            document.add(nombreDocumento);

            contenido.agregar(document);

            Paragraph fechaEmision = new Paragraph("Emitida el " + fecha(LocalDate.now()) + ".",
                    font(10, Font.NORMAL, Color.DARK_GRAY));
            fechaEmision.setSpacingBefore(24);
            fechaEmision.setAlignment(Element.ALIGN_RIGHT);
            document.add(fechaEmision);
            agregarFirmas(document);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la constancia PDF", exception);
        }
    }

    private PdfPTable tablaDetalles() {
        PdfPTable table = new PdfPTable(new float[]{1.0f, 2.3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(18);
        return table;
    }

    private void agregarDetalle(PdfPTable table, String etiqueta, String valor) {
        PdfPCell label = new PdfPCell(new Phrase(etiqueta, font(10, Font.BOLD)));
        label.setBackgroundColor(new Color(238, 243, 241));
        PdfPCell content = new PdfPCell(new Phrase(texto(valor, "No especificado"), font(10, Font.NORMAL)));
        for (PdfPCell cell : new PdfPCell[]{label, content}) {
            cell.setBorderColor(new Color(210, 218, 215));
            cell.setPadding(8);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }
        table.addCell(label);
        table.addCell(content);
    }

    private void agregarFirmas(Document document) throws Exception {
        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(58);
        for (String label : new String[]{"Firma y sello de la institución", "Firma del representante"}) {
            PdfPCell cell = new PdfPCell(new Phrase(label, font(9, Font.NORMAL, Color.DARK_GRAY)));
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColorTop(new Color(100, 110, 106));
            cell.setPaddingTop(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            signatures.addCell(cell);
        }
        document.add(signatures);
    }

    private String grado(Matricula matricula) {
        String especial = matricula.getSeccion().getGrado().getNombreEspecial();
        return texto(especial, matricula.getSeccion().getGrado().getNumeroGrado().toString());
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? "No registrada" : fecha.format(FORMATO_FECHA);
    }

    String periodoEscolar(Short anioInicio) {
        return anioInicio == null ? "No registrado" : anioInicio + "-" + (anioInicio + 1);
    }

    private String texto(String valor, String alternativa) {
        return valor == null || valor.isBlank() ? alternativa : valor;
    }

    private Font font(float size, int style) {
        return font(size, style, Color.BLACK);
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    @FunctionalInterface
    private interface ContenidoDocumento {
        void agregar(Document document) throws Exception;
    }
}