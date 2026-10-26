package com.tesis.service;

import com.tesis.entity.Matricula;
import com.tesis.entity.Representante;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class NominaEstudiantilPdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.institucion.nombre:C.E.N. Lcda. Roraima Asansa Escandela}")
    private String institucion;

    @Value("${app.institucion.municipio:San Francisco}")
    private String municipio;

    @Value("${app.institucion.estado:Zulia}")
    private String estado;

    @Value("${app.institucion.codigo-plantel:OD34942317}")
    private String codigoPlantel;

    private final Clock clock;

    public NominaEstudiantilPdfGenerator(Clock clock) {
        this.clock = clock;
    }

    public byte[] generar(Short anioEscolar,
                          List<Matricula> matriculas,
                          Map<Integer, Representante> representantes,
                          List<String> columnas) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER.rotate(), 25, 25, 28, 28);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(MembretePdf.crear(institucion, municipio, estado, codigoPlantel));

            org.openpdf.text.Paragraph titulo = new org.openpdf.text.Paragraph(
                    "NÓMINA ESTUDIANTIL · AÑO ESCOLAR " + anioEscolar,
                    new Font(Font.HELVETICA, 13, Font.BOLD));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(14);
            titulo.setSpacingAfter(12);
            document.add(titulo);

            float[] anchos = new float[columnas.size()];
            for (int indice = 0; indice < columnas.size(); indice++) {
                anchos[indice] = anchoColumna(columnas.get(indice));
            }
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            tabla.setSplitLate(false);

            for (String columna : columnas) {
                agregarCelda(tabla, encabezado(columna), true, columnas.size());
            }

            LocalDate hoy = LocalDate.now(clock);
            for (Matricula matricula : matriculas) {
                Representante representante = representantes.get(matricula.getEstudiante().getId());
                for (String columna : columnas) {
                    agregarCelda(tabla, valor(columna, matricula, representante, hoy),
                            false, columnas.size());
                }
            }

            if (matriculas.isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("No hay matrículas para los filtros seleccionados.",
                        new Font(Font.HELVETICA, 8, Font.NORMAL)));
                vacio.setColspan(columnas.size());
                vacio.setPadding(8);
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(vacio);
            }

            document.add(tabla);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la nómina estudiantil PDF", exception);
        }
    }

    private String encabezado(String columna) {
        return switch (columna) {
            case "nombre" -> "Nombre y apellido";
            case "cedula" -> "Cédula";
            case "fechaNacimiento" -> "Fecha de nacimiento";
            case "edad" -> "Edad";
            case "sexo" -> "Sexo";
            case "nivelGradoSeccion" -> "Nivel / grado / sección";
            case "tipoIngreso" -> "Tipo de ingreso";
            case "representanteNombre" -> "Representante principal";
            case "representanteCedula" -> "Cédula del representante";
            case "representanteTelefono" -> "Teléfono del representante";
            default -> throw new IllegalArgumentException("Columna de nómina desconocida: " + columna);
        };
    }

    private String valor(String columna, Matricula matricula, Representante representante, LocalDate hoy) {
        var estudiante = matricula.getEstudiante();
        return switch (columna) {
            case "nombre" -> texto(estudiante.getNombre()) + " " + texto(estudiante.getApellido());
            case "cedula" -> texto(estudiante.getCedula());
            case "fechaNacimiento" -> estudiante.getFechaNacimiento() == null
                    ? "—" : estudiante.getFechaNacimiento().format(FORMATO_FECHA);
            case "edad" -> edad(estudiante.getFechaNacimiento(), hoy);
            case "sexo" -> texto(estudiante.getSexo());
            case "nivelGradoSeccion" -> nivelGradoSeccion(matricula);
            case "tipoIngreso" -> tipoIngreso(matricula.getTipoIngreso());
            case "representanteNombre" -> representante == null ? "—"
                    : texto(representante.getNombre()) + " " + texto(representante.getApellido());
            case "representanteCedula" -> representante == null ? "—" : texto(representante.getCedula());
            case "representanteTelefono" -> representante == null ? "—" : texto(representante.getTelefono());
            default -> throw new IllegalArgumentException("Columna de nómina desconocida: " + columna);
        };
    }

    private String nivelGradoSeccion(Matricula matricula) {
        var seccion = matricula.getSeccion();
        var grado = seccion.getGrado();
        String nombreGrado = grado.getNombreEspecial() == null || grado.getNombreEspecial().isBlank()
                ? grado.getNumeroGrado() + "° grado" : grado.getNombreEspecial();
        return texto(grado.getNivelEducativo().getNombre()) + " · " + nombreGrado
                + " · Sección " + texto(seccion.getLetraSeccion());
    }

    private String tipoIngreso(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return switch (valor) {
            case "regular" -> "Regular";
            case "nuevo_ingreso" -> "Nuevo ingreso";
            case "reingreso" -> "Reingreso";
            case "traslado" -> "Traslado";
            default -> valor;
        };
    }

    private String edad(LocalDate nacimiento, LocalDate hoy) {
        if (nacimiento == null || nacimiento.isAfter(hoy)) {
            return "—";
        }
        return Integer.toString(Period.between(nacimiento, hoy).getYears());
    }

    private float anchoColumna(String columna) {
        return switch (columna) {
            case "nombre", "representanteNombre" -> 1.6f;
            case "nivelGradoSeccion" -> 2.2f;
            case "fechaNacimiento" -> 1.15f;
            case "representanteCedula" -> 1.35f;
            case "representanteTelefono" -> 1.25f;
            case "tipoIngreso" -> 1.15f;
            case "cedula" -> 1.0f;
            default -> 0.65f;
        };
    }

    private void agregarCelda(PdfPTable tabla, String valor, boolean encabezado, int cantidadColumnas) {
        int tamanoFuente = cantidadColumnas > 8 ? 6 : 7;
        PdfPCell celda = new PdfPCell(new Phrase(valor,
                new Font(Font.HELVETICA, tamanoFuente, encabezado ? Font.BOLD : Font.NORMAL)));
        celda.setPadding(4);
        celda.setBorderColor(new Color(210, 218, 215));
        if (encabezado) {
            celda.setBackgroundColor(new Color(238, 243, 241));
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }
        tabla.addCell(celda);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}
