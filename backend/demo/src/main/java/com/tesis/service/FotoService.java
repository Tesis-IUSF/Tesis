package com.tesis.service;

import com.tesis.dto.FotoResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class FotoService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private final Path uploadsDirectory;

    public FotoService(@Value("${app.uploads.dir:./uploads}") String uploadsDirectory) {
        this.uploadsDirectory = Path.of(uploadsDirectory).toAbsolutePath().normalize();
    }

    public FotoResponseDTO guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe adjuntar una imagen");
        }
        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "La imagen no puede superar 2 MB");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer la imagen enviada", exception);
        }

        FormatoImagen formato = detectarFormato(contenido);
        String nombre = UUID.randomUUID() + "." + formato.extension();
        try {
            Files.createDirectories(uploadsDirectory);
            Files.write(uploadsDirectory.resolve(nombre), contenido, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            log.error("No se pudo guardar la imagen cargada en {}", uploadsDirectory, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen", exception);
        }
        return new FotoResponseDTO("/api/fotos/" + nombre);
    }

    public Imagen almacenada(String nombre) {
        String extension = extensionValida(nombre);
        Path archivo = uploadsDirectory.resolve(nombre).normalize();
        if (!archivo.getParent().equals(uploadsDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        try {
            return new Imagen(Files.readAllBytes(archivo), FormatoImagen.porExtension(extension).contentType());
        } catch (java.nio.file.NoSuchFileException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        } catch (IOException exception) {
            log.error("No se pudo leer la imagen almacenada {}", nombre, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo leer la imagen", exception);
        }
    }

    private FormatoImagen detectarFormato(byte[] contenido) {
        if (tieneFirma(contenido, new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        }) && esImagenDecodificable(contenido)) {
            return FormatoImagen.PNG;
        }
        if (contenido.length >= 3
                && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8
                && (contenido[2] & 0xff) == 0xff
                && esImagenDecodificable(contenido)) {
            return FormatoImagen.JPG;
        }
        if (esWebp(contenido)) {
            return FormatoImagen.WEBP;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Formato de imagen no válido; solo se aceptan JPG, PNG o WebP");
    }

    private boolean esImagenDecodificable(byte[] contenido) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            return imagen != null && imagen.getWidth() > 0 && imagen.getHeight() > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean esWebp(byte[] contenido) {
        if (contenido.length < 30
                || !tieneTexto(contenido, 0, "RIFF")
                || !tieneTexto(contenido, 8, "WEBP")
                || enteroLittleEndian(contenido, 4) != contenido.length - 8L) {
            return false;
        }

        boolean tieneImagen = false;
        int offset = 12;
        while (offset + 8 <= contenido.length) {
            String chunk = new String(contenido, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            long longitud = enteroLittleEndian(contenido, offset + 4);
            long inicioDatos = offset + 8L;
            long finDatos = inicioDatos + longitud;
            if (finDatos > contenido.length) {
                return false;
            }

            if ("VP8 ".equals(chunk) && longitud >= 10
                    && contenido[(int) inicioDatos + 3] == (byte) 0x9d
                    && contenido[(int) inicioDatos + 4] == 0x01
                    && contenido[(int) inicioDatos + 5] == 0x2a) {
                tieneImagen = true;
            } else if ("VP8L".equals(chunk) && longitud >= 5
                    && contenido[(int) inicioDatos] == 0x2f) {
                tieneImagen = true;
            }

            offset = (int) (finDatos + (longitud & 1));
        }
        return offset == contenido.length && tieneImagen;
    }

    private boolean tieneFirma(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int indice = 0; indice < firma.length; indice++) {
            if (contenido[indice] != firma[indice]) {
                return false;
            }
        }
        return true;
    }

    private boolean tieneTexto(byte[] contenido, int offset, String texto) {
        byte[] esperado = texto.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (contenido.length < offset + esperado.length) {
            return false;
        }
        for (int indice = 0; indice < esperado.length; indice++) {
            if (contenido[offset + indice] != esperado[indice]) {
                return false;
            }
        }
        return true;
    }

    private long enteroLittleEndian(byte[] contenido, int offset) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(contenido, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    private String extensionValida(String nombre) {
        if (nombre == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        String[] partes = nombre.toLowerCase(Locale.ROOT).split("\\.", -1);
        if (partes.length != 2 || !FormatoImagen.esExtension(partes[1])) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        try {
            UUID id = UUID.fromString(partes[0]);
            if (!id.toString().equals(partes[0])) {
                throw new IllegalArgumentException("UUID no canónico");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        return partes[1];
    }

    public record Imagen(byte[] contenido, String contentType) {
    }

    private enum FormatoImagen {
        JPG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        FormatoImagen(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        private String extension() {
            return extension;
        }

        private String contentType() {
            return contentType;
        }

        private static boolean esExtension(String extension) {
            for (FormatoImagen formato : values()) {
                if (formato.extension.equals(extension)) {
                    return true;
                }
            }
            return false;
        }

        private static FormatoImagen porExtension(String extension) {
            for (FormatoImagen formato : values()) {
                if (formato.extension.equals(extension)) {
                    return formato;
                }
            }
            throw new IllegalArgumentException("Extensión de imagen no soportada");
        }
    }
}
