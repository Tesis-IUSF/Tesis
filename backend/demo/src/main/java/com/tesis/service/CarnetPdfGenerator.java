package com.tesis.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfWriter;
import com.tesis.entity.Empleado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

@Component
public class CarnetPdfGenerator {

    private static final int QR_SIZE = 320;
    private static final float CARD_WIDTH = 242.65f;
    private static final float CARD_HEIGHT = 153.07f;

    @Value("${app.institucion.nombre:Sistema Educativo}")
    private String institucion;

    public byte[] generar(Empleado empleado, String qrToken) {
        try {
            byte[] qrImage = generarImagenQr(qrToken);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(new Rectangle(CARD_WIDTH, CARD_HEIGHT), 12, 12, 10, 10);
            PdfWriter.getInstance(document, output);
            document.open();

            PdfPTable card = new PdfPTable(new float[]{1.8f, 1.0f});
            card.setWidthPercentage(100);
            card.setSpacingBefore(0);

            PdfPCell header = new PdfPCell(new Phrase(institucion, font(11, Font.BOLD, Color.WHITE)));
            header.setColspan(2);
            header.setBackgroundColor(new Color(28, 79, 72));
            header.setBorder(Rectangle.NO_BORDER);
            header.setPadding(8);
            header.setHorizontalAlignment(Element.ALIGN_LEFT);
            card.addCell(header);

            PdfPCell details = new PdfPCell();
            details.setBorder(Rectangle.NO_BORDER);
            details.setPaddingTop(9);
            details.addElement(new Paragraph("CARNET DE PERSONAL", font(8, Font.BOLD, new Color(28, 79, 72))));
            details.addElement(new Paragraph(
                    empleado.getNombre() + " " + empleado.getApellido(), font(11, Font.BOLD, Color.BLACK)));
            details.addElement(new Paragraph("ID: " + empleado.getId(), font(8, Font.NORMAL, Color.DARK_GRAY)));
            details.addElement(new Paragraph("Cargo: " + texto(empleado.getCargo() == null
                    ? null : empleado.getCargo().getNombreCargo()), font(7, Font.NORMAL, Color.DARK_GRAY)));
            details.addElement(new Paragraph("Departamento: " + texto(empleado.getDepartamento() == null
                    ? null : empleado.getDepartamento().getNombre()), font(7, Font.NORMAL, Color.DARK_GRAY)));
            details.addElement(new Paragraph("QR para registro de asistencia", font(6, Font.NORMAL, Color.GRAY)));
            card.addCell(details);

            Image qr = Image.getInstance(qrImage);
            qr.scaleToFit(78, 78);
            PdfPCell qrCell = new PdfPCell(qr, false);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setPaddingTop(8);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            card.addCell(qrCell);

            document.add(card);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el carnet PDF", exception);
        }
    }

    public byte[] unirCarnets(List<byte[]> carnets) {
        if (carnets == null || carnets.isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un carnet PDF");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document();
            PdfCopy copy = new PdfCopy(document, output);
            document.open();
            for (byte[] carnet : carnets) {
                PdfReader reader = new PdfReader(carnet);
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }
                reader.close();
            }
            document.close();
            copy.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudieron unir los carnets PDF", exception);
        }
    }

    private byte[] generarImagenQr(String token) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = new MultiFormatWriter().encode(
                token, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return output.toByteArray();
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "No asignado" : valor;
    }
}