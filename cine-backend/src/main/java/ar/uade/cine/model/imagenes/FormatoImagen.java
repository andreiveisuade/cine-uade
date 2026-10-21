package ar.uade.cine.model.imagenes;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

// Los formatos que se pueden subir, reconocidos por sus primeros bytes; la extensión la elige quien sube.
@Getter
@Accessors(fluent = true)
public enum FormatoImagen {

    JPG("jpg", "image/jpeg", "ffd8ff"),
    PNG("png", "image/png", "89504e470d0a1a0a"),
    // RIFF, cuatro bytes de largo y WEBP: RIFF solo también es WAV o AVI.
    WEBP("webp", "image/webp", "52494646", 8, "57454250");

    private final String extension;
    private final String tipoDeContenido;
    @Getter(AccessLevel.NONE)
    private final byte[] inicio;
    @Getter(AccessLevel.NONE)
    private final int desplazamiento;
    @Getter(AccessLevel.NONE)
    private final byte[] marca;

    FormatoImagen(String extension, String tipoDeContenido, String inicio) {
        this(extension, tipoDeContenido, inicio, 0, "");
    }

    FormatoImagen(String extension, String tipoDeContenido, String inicio, int desplazamiento, String marca) {
        this.extension = extension;
        this.tipoDeContenido = tipoDeContenido;
        this.inicio = HexFormat.of().parseHex(inicio);
        this.desplazamiento = desplazamiento;
        this.marca = HexFormat.of().parseHex(marca);
    }

    public static Optional<FormatoImagen> de(byte[] contenido) {
        return Arrays.stream(values()).filter(formato -> formato.reconoce(contenido)).findFirst();
    }

    public static Optional<FormatoImagen> conExtension(String extension) {
        return Arrays.stream(values()).filter(formato -> formato.extension.equals(extension)).findFirst();
    }

    private boolean reconoce(byte[] contenido) {
        return empiezaCon(contenido, 0, inicio) && empiezaCon(contenido, desplazamiento, marca);
    }

    private static boolean empiezaCon(byte[] contenido, int desde, byte[] esperado) {
        return contenido.length >= desde + esperado.length
                && Arrays.equals(contenido, desde, desde + esperado.length, esperado, 0, esperado.length);
    }
}
