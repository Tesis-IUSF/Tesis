package com.tesis.service;

import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NominaPersonalPdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] ENCABEZADOS = {
        "Cédula", "Fecha de nacimiento", "Sexo", "Correo", "Teléfono",
        "Cargo", "Departamento", "Turno", "Fecha de ingreso"
    };

    @Value("${app.institucion.nombre:C.E.N. Lcda. Roraima Asansa Escandela}")
    private String institucion;

    @Value("${app.institucion.municipio:San Francisco}")
    private String municipio;

    @Value("${app.institucion.estado:Zulia}")
    private String estado;

    @Value("${app.institucion.codigo-plantel:OD34942317}")
    private String codigoPlantel;

    public byte[] generar(List<EmpleadoResponseDTO> empleados, boolean incluirFechaEgreso) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER.rotate(), 28, 28, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(encabezado());

            Paragraph titulo = new Paragraph("NÓMINA DE PERSONAL",
                    new Font(Font.HELVETICA, 13, Font.BOLD));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(18);
            titulo.setSpacingAfter(14);
            document.add(titulo);

            float[] anchos = incluirFechaEgreso
                    ? new float[] {1.15f, 1.15f, 0.55f, 1.7f, 1.0f, 1.3f, 1.3f, 1.1f, 1.15f, 1.15f}
                    : new float[] {1.2f, 1.2f, 0.6f, 1.8f, 1.05f, 1.35f, 1.35f, 1.15f, 1.2f};
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            tabla.setSplitLate(false);

            for (String encabezado : ENCABEZADOS) {
                agregarCelda(tabla, encabezado, true);
            }
            if (incluirFechaEgreso) {
                agregarCelda(tabla, "Fecha de egreso", true);
            }

            for (EmpleadoResponseDTO empleado : empleados) {
                agregarCelda(tabla, texto(empleado.getCedula()), false);
                agregarCelda(tabla, fecha(empleado.getFechaNacimiento()), false);
                agregarCelda(tabla, texto(empleado.getSexo()), false);
                agregarCelda(tabla, texto(empleado.getCorreo()), false);
                agregarCelda(tabla, texto(empleado.getTelefono()), false);
                agregarCelda(tabla, texto(empleado.getCargoNombre()), false);
                agregarCelda(tabla, texto(empleado.getDepartamentoNombre()), false);
                agregarCelda(tabla, texto(empleado.getTurnoNombre()), false);
                agregarCelda(tabla, fecha(empleado.getFechaIngreso()), false);
                if (incluirFechaEgreso) {
                    agregarCelda(tabla, fecha(empleado.getFechaEgreso()), false);
                }
            }

            document.add(tabla);
            Paragraph fechaGeneracion = new Paragraph(
                    "Generado el " + LocalDate.now().format(FORMATO_FECHA),
                    new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY));
            fechaGeneracion.setAlignment(Element.ALIGN_RIGHT);
            fechaGeneracion.setSpacingBefore(8);
            document.add(fechaGeneracion);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la nómina de personal PDF", exception);
        }
    }

    private PdfPTable encabezado() throws Exception {
        PdfPTable tabla = new PdfPTable(new float[] {1.35f, 2.4f, 0.9f});
        tabla.setWidthPercentage(100);

        tabla.addCell(celdaImagen("logo-ministerio.png", 135, 48, Element.ALIGN_LEFT));

        Paragraph centro = new Paragraph();
        centro.setAlignment(Element.ALIGN_CENTER);
        centro.setLeading(0, 1.3f);
        centro.add(new Phrase("República Bolivariana de Venezuela\n", fuenteMembrete()));
        centro.add(new Phrase("Ministerio del Poder Popular para la Educación\n", fuenteMembrete()));
        centro.add(new Phrase(institucion + "\n", fuenteMembrete()));
        centro.add(new Phrase("Municipio " + municipio + " – Estado " + estado + "\n", fuenteMembrete()));
        centro.add(new Phrase("Cod. Plantel: " + codigoPlantel, fuenteMembrete()));
        PdfPCell celdaCentro = new PdfPCell();
        celdaCentro.addElement(centro);
        celdaCentro.setBorder(Rectangle.NO_BORDER);
        celdaCentro.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(celdaCentro);

        tabla.addCell(celdaImagen("escudo.png", 58, 68, Element.ALIGN_RIGHT));
        tabla.setSpacingAfter(6);
        return tabla;
    }

    private PdfPCell celdaImagen(String archivo, float ancho, float alto, int alineacion) throws Exception {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setHorizontalAlignment(alineacion);
        try (InputStream recurso = new ClassPathResource("constancias/" + archivo).getInputStream()) {
            Image imagen = Image.getInstance(recurso.readAllBytes());
            imagen.scaleToFit(ancho, alto);
            celda.addElement(imagen);
            celda.setPadding(0);
        }
        return celda;
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
        return fecha == null ? "—" : fecha.format(FORMATO_FECHA);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private Font fuenteMembrete() {
        return new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(35, 35, 35));
    }
}
