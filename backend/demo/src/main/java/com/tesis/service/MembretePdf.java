package com.tesis.service;

import org.openpdf.text.Chunk;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.io.InputStream;

final class MembretePdf {

    private MembretePdf() {
    }

    static PdfPTable crear(String institucion, String municipio, String estado, String codigoPlantel)
            throws Exception {
        PdfPTable tabla = new PdfPTable(new float[] {1.2f, 3.6f, 1.2f});
        tabla.setWidthPercentage(100);

        tabla.addCell(celdaImagen("logo-ministerio.png", 135, 48, Element.ALIGN_LEFT));

        Paragraph centro = new Paragraph();
        centro.setAlignment(Element.ALIGN_CENTER);
        centro.setLeading(0, 1.3f);
        centro.add(new Chunk("República Bolivariana de Venezuela\n", fuente()));
        centro.add(new Chunk("Ministerio del Poder Popular para la Educación\n", fuente()));
        centro.add(new Chunk(institucion + "\n", fuente()));
        centro.add(new Chunk("Municipio " + municipio + " – Estado " + estado + "\n", fuente()));
        centro.add(new Chunk("Cod. Plantel: " + codigoPlantel, fuente()));
        PdfPCell celdaCentro = new PdfPCell();
        celdaCentro.addElement(centro);
        celdaCentro.setBorder(Rectangle.NO_BORDER);
        celdaCentro.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(celdaCentro);

        tabla.addCell(celdaImagen("escudo.png", 58, 68, Element.ALIGN_RIGHT));
        tabla.setSpacingAfter(6);
        return tabla;
    }

    private static PdfPCell celdaImagen(String archivo, float ancho, float alto, int alineacion)
            throws Exception {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setHorizontalAlignment(alineacion);
        try (InputStream recurso = new ClassPathResource("constancias/" + archivo).getInputStream()) {
            Image imagen = Image.getInstance(recurso.readAllBytes());
            imagen.scaleToFit(ancho, alto);
            imagen.setAlignment(alineacion);
            celda.addElement(imagen);
            celda.setPadding(0);
        }
        return celda;
    }

    private static Font fuente() {
        return new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(35, 35, 35));
    }
}
