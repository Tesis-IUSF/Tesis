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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

@Component
public class CarnetPdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(CarnetPdfGenerator.class);
    private static final int QR_SIZE = 320;
    private static final float CARD_WIDTH = 242.65f;
    private static final float CARD_HEIGHT = 153.07f;
    private static final Pattern FOTO_URL = Pattern.compile(
            "^/api/fotos/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(?:jpg|png|webp))$",
            Pattern.CASE_INSENSITIVE);

    private final FotoService fotoService;
    private final String institucion;

    public CarnetPdfGenerator(
            FotoService fotoService,
            @Value("${app.institucion.nombre:Sistema Educativo}") String institucion) {
        this.fotoService = fotoService;
        this.institucion = institucion;
    }

    public byte[] generar(Empleado empleado, String qrToken) {
        try {
            byte[] qrImage = generarImagenQr(qrToken);
            byte[] foto = cargarFoto(empleado);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(new Rectangle(CARD_WIDTH, CARD_HEIGHT), 12, 12, 10, 10);
            PdfWriter.getInstance(document, output);
            document.open();

            PdfPTable card = new PdfPTable(new float[]{0.8f, 2.0f, 0.9f});
            card.setWidthPercentage(100);
            card.setSpacingBefore(0);

            PdfPCell header = new PdfPCell(new Phrase(institucion, font(11, Font.BOLD, Color.WHITE)));
            header.setColspan(3);
            header.setBackgroundColor(new Color(28, 79, 72));
            header.setBorder(Rectangle.NO_BORDER);
            header.setPadding(8);
            header.setHorizontalAlignment(Element.ALIGN_LEFT);
            card.addCell(header);

            PdfPCell headerGap = new PdfPCell();
            headerGap.setColspan(3);
            headerGap.setFixedHeight(6);
            headerGap.setBorder(Rectangle.NO_BORDER);
            card.addCell(headerGap);

            card.addCell(celdaFoto(empleado, foto));

            PdfPCell details = new PdfPCell();
            details.setBorder(Rectangle.NO_BORDER);
            details.setPaddingTop(7);
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
            qr.scaleToFit(50, 50);
            PdfPCell qrCell = new PdfPCell(qr, false);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setPaddingTop(7);
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

    private PdfPCell celdaFoto(Empleado empleado, byte[] contenido) throws Exception {
        PdfPCell celda;
        if (contenido != null) {
            Image foto = Image.getInstance(contenido);
            foto.scaleToFit(38, 54);
            celda = new PdfPCell(foto, false);
        } else {
            celda = new PdfPCell();
            celda.setBackgroundColor(new Color(238, 241, 240));
            celda.addElement(new Paragraph(iniciales(empleado),
                    font(17, Font.BOLD, new Color(28, 79, 72))));
            celda.addElement(new Paragraph("SIN FOTO", font(5, Font.BOLD, Color.GRAY)));
        }
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(new Color(218, 224, 222));
        celda.setBorderWidth(0.5f);
        celda.setPadding(4);
        celda.setPaddingTop(6);
        celda.setFixedHeight(70);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private byte[] cargarFoto(Empleado empleado) throws IOException {
        String fotoUrl = empleado.getFotoUrl();
        if (fotoUrl == null || fotoUrl.isBlank()) {
            return null;
        }

        Matcher matcher = FOTO_URL.matcher(fotoUrl);
        if (!matcher.matches()) {
            log.warn("Se omitió una foto con URL inválida al generar el carnet del empleado {}",
                    empleado.getId());
            return null;
        }

        try {
            byte[] archivo = fotoService.almacenada(matcher.group(1)).contenido();
            BufferedImage imagen = ImageIO.read(new java.io.ByteArrayInputStream(archivo));
            if (imagen == null) {
                log.warn("No se pudo decodificar la foto del empleado {}; se usará el marcador sin foto",
                        empleado.getId());
                return null;
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            if (!ImageIO.write(imagen, "png", png)) {
                log.warn("No se pudo convertir la foto del empleado {}; se usará el marcador sin foto",
                        empleado.getId());
                return null;
            }
            return png.toByteArray();
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("No se encontró el archivo de foto del empleado {}; se usará el marcador sin foto",
                        empleado.getId());
                return null;
            }
            throw exception;
        }
    }

    private String iniciales(Empleado empleado) {
        String nombre = primeraLetra(empleado.getNombre());
        String apellido = primeraLetra(empleado.getApellido());
        return nombre + apellido;
    }

    private String primeraLetra(String texto) {
        return texto == null || texto.isBlank()
                ? "?" : texto.strip().substring(0, 1).toUpperCase(Locale.ROOT);
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