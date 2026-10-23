package com.tesis.service;

import com.tesis.entity.Matricula;
import com.tesis.entity.RetiroMatricula;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera las constancias en PDF con el formato oficial del plantel.
 * Los datos de la institución y del director son configurables (app.institucion.*),
 * con los valores reales como predeterminados.
 */
@Component
public class ConstanciaPdfGenerator {

    private static final Locale ES = Locale.forLanguageTag("es-VE");
    private static final ZoneId ZONA = ZoneId.of("America/Caracas");
    private static final Pattern CEDULA = Pattern.compile("^([VEve])?[-\\s]?(\\d{1,10})$");

    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] UNIDADES = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis",
            "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince",
            "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidós",
            "veintitrés", "veinticuatro", "veinticinco", "veintiséis", "veintisiete", "veintiocho",
            "veintinueve"};
    private static final String[] DECENAS = {"", "", "", "treinta", "cuarenta", "cincuenta",
            "sesenta", "setenta", "ochenta", "noventa"};
    private static final String[] CENTENAS = {"", "ciento", "doscientos", "trescientos",
            "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};

    @Value("${app.institucion.nombre:C.E.N. Lcda. Roraima Asansa Escandela}")
    private String institucion;

    @Value("${app.institucion.nombre-completo:COMPLEJO EDUCATIVO NACIONAL LCDA. RORAIMA ASANSA ESCANDELA}")
    private String institucionCompleta;

    @Value("${app.institucion.codigo-plantel:OD34942317}")
    private String codigoPlantel;

    @Value("${app.institucion.rif:J508197682}")
    private String rif;

    @Value("${app.institucion.municipio:San Francisco}")
    private String municipio;

    @Value("${app.institucion.estado:Zulia}")
    private String estado;

    @Value("${app.institucion.direccion:Km 9,5 Vía Perijá, Urbanización Villa Chinita, Av. 1 Parroquia José Domingo Rus}")
    private String direccion;

    @Value("${app.institucion.correo:ceroraimasansaescandela@gmail.com}")
    private String correo;

    @Value("${app.institucion.telefono:0424-6772242}")
    private String telefono;

    @Value("${app.institucion.director.nombre:MgS. Javier Mavares}")
    private String directorNombre;

    @Value("${app.institucion.director.cedula:V-17.835.835}")
    private String directorCedula;

    @Value("${app.institucion.director.cargo:Director}")
    private String directorCargo;

    // ------------------------------------------------------------------
    // Constancias
    // ------------------------------------------------------------------

    public byte[] generarInscripcion(Matricula matricula) {
        return generar("CONSTANCIA DE INSCRIPCIÓN", false, document -> {
            agregarPresentacion(document);
            StringBuilder texto = new StringBuilder("Que el(la) estudiante ")
                    .append(datosEstudiante(matricula))
                    .append(", ").append(nacimiento(matricula, true))
                    .append("se encuentra inscrito(a) en este complejo y cursa ")
                    .append(cursoYNivel(matricula))
                    .append(" en el Período Escolar ").append(periodoEscolar(matricula.getAnioEscolar()))
                    .append(".");
            if (matricula.getFechaFormalizacion() != null) {
                texto.append(" La inscripción fue formalizada el ")
                        .append(fechaLarga(matricula.getFechaFormalizacion(), true, true)).append(".");
            }
            document.add(parrafo(texto.toString()));
        });
    }

    public byte[] generarAsignacionCupo(Matricula matricula) {
        return generar("CONSTANCIA DE ASIGNACIÓN DE CUPO", true, document -> {
            agregarPresentacion(document);
            document.add(parrafo("Que el(la) estudiante " + datosEstudiante(matricula)
                    + ", tiene asignado un cupo en este complejo para cursar "
                    + cursoYNivel(matricula) + " durante el Período Escolar "
                    + periodoEscolar(matricula.getAnioEscolar()) + "."));
        });
    }

    public byte[] generarRetiro(RetiroMatricula retiro) {
        Matricula matricula = retiro.getMatricula();
        return generar("CONSTANCIA DE BUENA CONDUCTA Y RETIRO", false, document -> {
            agregarPresentacion(document);
            String solicitante = (limpio(retiro.getSolicitanteNombre()) + " "
                    + limpio(retiro.getSolicitanteApellido())).trim().toUpperCase(ES);
            document.add(parrafo("Que el(la) estudiante " + datosEstudiante(matricula)
                    + ", " + nacimiento(matricula, false)
                    + "cursando " + cursoYNivel(matricula)
                    + " en el Período Escolar " + periodoEscolar(matricula.getAnioEscolar())
                    + ", demostrando una **BUENA CONDUCTA** dentro del complejo. Así mismo, el(la) "
                    + "estudiante fue retirado(a) de la institución el día "
                    + fechaLarga(retiro.getFechaRetiro(), false, false) + ", por la siguiente causa: **"
                    + limpio(retiro.getMotivo()).toUpperCase(ES) + "**, según lo manifestado por su "
                    + "representante **" + solicitante + "**, titular de la Cédula de Identidad N° **"
                    + formatearCedula(retiro.getSolicitanteCedula()) + "**."));
        });
    }

    // ------------------------------------------------------------------
    // Estructura del documento
    // ------------------------------------------------------------------

    private byte[] generar(String titulo, boolean mostrarRif, ContenidoDocumento contenido) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // Márgenes: el inferior deja libre el pie de página (línea y datos de contacto)
            Document document = new Document(PageSize.LETTER, 64, 64, 30, 100);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PiePagina());
            document.open();

            document.add(encabezado(mostrarRif));

            Paragraph nombreDocumento = new Paragraph(titulo, font(12.5f, Font.BOLD));
            nombreDocumento.setAlignment(Element.ALIGN_CENTER);
            nombreDocumento.setSpacingBefore(26);
            nombreDocumento.setSpacingAfter(20);
            document.add(nombreDocumento);

            contenido.agregar(document);

            document.add(parrafo(expedicion()));
            agregarFirma(document);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la constancia PDF", exception);
        }
    }

    /** Logo del Ministerio a la izquierda, datos del plantel al centro y escudo a la derecha. */
    private PdfPTable encabezado(boolean mostrarRif) throws Exception {
        PdfPTable tabla = new PdfPTable(new float[]{1.35f, 2.4f, 0.9f});
        tabla.setWidthPercentage(100);

        tabla.addCell(celdaImagen("logo-ministerio.png", 135, 48, Element.ALIGN_LEFT));

        Paragraph centro = new Paragraph();
        centro.setAlignment(Element.ALIGN_CENTER);
        centro.setLeading(0, 1.3f);
        centro.add(new Chunk("República Bolivariana de Venezuela\n", font(10, Font.NORMAL)));
        centro.add(new Chunk("Ministerio del Poder Popular para la Educación\n", font(10, Font.NORMAL)));
        centro.add(new Chunk(institucion + "\n", font(10, Font.NORMAL)));
        centro.add(new Chunk("Municipio " + municipio + " – Estado " + estado + "\n", font(10, Font.NORMAL)));
        centro.add(new Chunk("Cod. Plantel: " + codigoPlantel
                + (mostrarRif ? "\nRIF: " + rif : ""), font(10, Font.NORMAL)));
        PdfPCell celdaCentro = new PdfPCell();
        celdaCentro.addElement(centro);
        celdaCentro.setBorder(Rectangle.NO_BORDER);
        celdaCentro.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(celdaCentro);

        tabla.addCell(celdaImagen("escudo.png", 58, 68, Element.ALIGN_RIGHT));
        return tabla;
    }

    private PdfPCell celdaImagen(String archivo, float ancho, float alto, int alineacion) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setHorizontalAlignment(alineacion);
        byte[] bytes = recurso(archivo);
        if (bytes != null) {
            try {
                Image imagen = Image.getInstance(bytes);
                imagen.scaleToFit(ancho, alto);
                celda.addElement(imagen);
                celda.setPadding(0);
            } catch (Exception ignorada) {
                // Sin imagen la constancia se genera igual
            }
        }
        return celda;
    }

    private byte[] recurso(String archivo) {
        try (InputStream in = new ClassPathResource("constancias/" + archivo).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /** "Quien suscribe, ..." y la fórmula "HACE CONSTAR:". */
    private void agregarPresentacion(Document document) throws Exception {
        document.add(parrafo("Quien suscribe, **" + directorNombre + "**, titular de la Cédula de "
                + "Identidad N° **" + directorCedula + "**, en calidad de **"
                + directorCargo.toUpperCase(ES) + "** del **" + institucionCompleta
                + "**, ubicado en " + direccion + ", Municipio " + municipio
                + " del Estado " + estado + "."));

        Paragraph constar = new Paragraph("HACE CONSTAR:", font(12, Font.BOLD));
        constar.setAlignment(Element.ALIGN_CENTER);
        constar.setSpacingBefore(8);
        constar.setSpacingAfter(12);
        document.add(constar);
    }

    private void agregarFirma(Document document) throws Exception {
        Paragraph atentamente = new Paragraph("Atentamente,", font(12, Font.BOLD));
        atentamente.setAlignment(Element.ALIGN_CENTER);
        atentamente.setSpacingBefore(6);
        document.add(atentamente);

        Paragraph linea = new Paragraph("______________________________", font(12, Font.NORMAL));
        linea.setAlignment(Element.ALIGN_CENTER);
        linea.setSpacingBefore(30);
        document.add(linea);

        for (String texto : new String[]{directorNombre, directorCedula, directorCargo, telefono}) {
            Paragraph p = new Paragraph(texto, font(11, Font.BOLD));
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);
        }
    }

    /** Línea gruesa y datos de contacto al pie de cada página. */
    private class PiePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(2.2f);
            cb.moveTo(document.left(), 74);
            cb.lineTo(document.right(), 74);
            cb.stroke();

            float centro = (document.left() + document.right()) / 2;
            Font fuente = font(8.5f, Font.ITALIC);
            String[] lineas = {
                    correo + "     tlf. " + telefono,
                    direccion,
                    "Municipio " + municipio + " del Estado " + estado
            };
            float y = 60;
            for (String linea : lineas) {
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(linea, fuente), centro, y, 0);
                y -= 11;
            }
        }
    }

    // ------------------------------------------------------------------
    // Textos
    // ------------------------------------------------------------------

    /** Párrafo justificado; el texto entre ** ** se escribe en negrita. */
    private Paragraph parrafo(String marcado) {
        Paragraph parrafo = new Paragraph();
        parrafo.setAlignment(Element.ALIGN_JUSTIFIED);
        parrafo.setLeading(0, 1.3f);
        parrafo.setFirstLineIndent(28);
        parrafo.setSpacingAfter(11);
        String[] partes = marcado.split("\\*\\*", -1);
        for (int i = 0; i < partes.length; i++) {
            parrafo.add(new Chunk(partes[i], font(12, i % 2 == 1 ? Font.BOLD : Font.NORMAL)));
        }
        return parrafo;
    }

    private String datosEstudiante(Matricula matricula) {
        String nombre = (limpio(matricula.getEstudiante().getNombre()) + " "
                + limpio(matricula.getEstudiante().getApellido())).trim().toUpperCase(ES);
        return "**" + nombre + "**, titular de la Cédula de Identidad N° **"
                + formatearCedula(matricula.getEstudiante().getCedula()) + "**";
    }

    /** "nacido(a) el ocho (8) de octubre de dos mil diecinueve (2019), " (con coma y espacio finales). */
    private String nacimiento(Matricula matricula, boolean conCero) {
        LocalDate nacimiento = matricula.getEstudiante().getFechaNacimiento();
        return nacimiento == null ? ""
                : "nacido(a) el " + fechaLarga(nacimiento, conCero, true) + ", ";
    }

    /** "el 2do año, Sección “B”, de Educación Media General". */
    private String cursoYNivel(Matricula matricula) {
        String especial = matricula.getSeccion().getGrado().getNombreEspecial();
        String grado = hayTexto(especial)
                ? especial.trim().toLowerCase(ES)
                : matricula.getSeccion().getGrado().getNumeroGrado() + " año";
        return "el " + grado + ", Sección “" + limpio(matricula.getSeccion().getLetraSeccion())
                + "”, de " + limpio(matricula.getSeccion().getGrado().getNivelEducativo().getNombre());
    }

    private String expedicion() {
        LocalDate hoy = LocalDate.now(ZONA);
        int dia = hoy.getDayOfMonth();
        String cuando = dia == 1
                ? "al primer (01) día"
                : "a los " + apocopar(enLetras(dia)) + " (" + dosDigitos(dia) + ") días";
        return "Constancia que se expide en " + municipio + ", " + cuando + " del mes de "
                + MESES[hoy.getMonthValue() - 1] + " del " + hoy.getYear() + ".";
    }

    /**
     * "uno (01) de septiembre de dos mil trece (2013)".
     * conCero: escribe el día como (01) o (1). anioEnLetras: "dos mil trece (2013)" o solo "2013".
     */
    private String fechaLarga(LocalDate fecha, boolean conCero, boolean anioEnLetras) {
        if (fecha == null) {
            return "(fecha no registrada)";
        }
        int dia = fecha.getDayOfMonth();
        String anio = anioEnLetras
                ? enLetras(fecha.getYear()) + " (" + fecha.getYear() + ")"
                : String.valueOf(fecha.getYear());
        return enLetras(dia) + " (" + (conCero ? dosDigitos(dia) : String.valueOf(dia)) + ") de "
                + MESES[fecha.getMonthValue() - 1] + " de " + anio;
    }

    /** "31000001" -> "V-31.000.001". Si ya trae otro formato, se respeta. */
    String formatearCedula(String cedula) {
        if (!hayTexto(cedula)) {
            return "No registrada";
        }
        Matcher m = CEDULA.matcher(cedula.trim());
        if (!m.matches()) {
            return cedula.trim().toUpperCase(ES);
        }
        String letra = m.group(1) == null ? "V" : m.group(1).toUpperCase(ES);
        String digitos = String.format(ES, "%,d", Long.parseLong(m.group(2))).replace(',', '.');
        return letra + "-" + digitos;
    }

    String periodoEscolar(Short anioInicio) {
        return anioInicio == null ? "No registrado" : anioInicio + "-" + (anioInicio + 1);
    }

    // ------------------------------------------------------------------
    // Números en letras (0 a 9999)
    // ------------------------------------------------------------------

    static String enLetras(int n) {
        if (n < 30) {
            return UNIDADES[n];
        }
        if (n < 100) {
            int d = n / 10;
            int u = n % 10;
            return u == 0 ? DECENAS[d] : DECENAS[d] + " y " + UNIDADES[u];
        }
        if (n == 100) {
            return "cien";
        }
        if (n < 1000) {
            int r = n % 100;
            return CENTENAS[n / 100] + (r == 0 ? "" : " " + enLetras(r));
        }
        int mil = n / 1000;
        int r = n % 1000;
        String miles = mil == 1 ? "mil" : enLetras(mil) + " mil";
        return miles + (r == 0 ? "" : " " + enLetras(r));
    }

    /** "veintiuno" -> "veintiún", "treinta y uno" -> "treinta y un" (antes de un sustantivo). */
    private static String apocopar(String numero) {
        if (numero.equals("veintiuno")) {
            return "veintiún";
        }
        if (numero.equals("uno")) {
            return "un";
        }
        return numero.endsWith(" uno") ? numero.substring(0, numero.length() - 1) : numero;
    }

    private static String dosDigitos(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Evita que datos con ** alteren el formato en negrita. */
    private String limpio(String valor) {
        return valor == null ? "" : valor.replace("**", "").trim();
    }

    private Font font(float size, int style) {
        return new Font(Font.HELVETICA, size, style, Color.BLACK);
    }

    @FunctionalInterface
    private interface ContenidoDocumento {
        void agregar(Document document) throws Exception;
    }
}