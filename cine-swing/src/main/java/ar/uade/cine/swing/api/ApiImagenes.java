package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.imagenes.Imagen;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

// Las imágenes de promos y productos: se suben acá y lo que se guarda en ellos es la URL que devuelve el backend.
@RequiredArgsConstructor
public final class ApiImagenes {

    private final ClienteHttp http;

    /** El tipo sale de la extensión solo para el multipart: el backend la valida por contenido. */
    public String subir(Path archivo) {
        byte[] datos;
        try {
            datos = Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new ErrorApi(0, "No se pudo leer el archivo " + archivo.getFileName());
        }
        String nombre = archivo.getFileName().toString();
        return http.subir("/imagenes", "archivo", nombre, tipo(nombre), datos, Imagen.class).url();
    }

    /** La URL es la que guardó el backend, {@code /api/imagenes/...}; el cliente ya apunta a {@code /api}. */
    public byte[] descargar(String url) {
        return http.bytes(url.replaceFirst("^/api", ""));
    }

    private static String tipo(String nombre) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        if (minusculas.endsWith(".png")) return "image/png";
        if (minusculas.endsWith(".webp")) return "image/webp";
        if (minusculas.endsWith(".jpg") || minusculas.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
