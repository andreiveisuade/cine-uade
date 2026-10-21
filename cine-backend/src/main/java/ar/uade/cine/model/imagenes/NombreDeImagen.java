package ar.uade.cine.model.imagenes;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Nombre de archivo de una imagen subida: uuid y extensión de su formato; Value Object.
// Un uuid nuevo por subida hace al archivo inmutable, y la forma fija deja afuera «..» y «/».
public record NombreDeImagen(String valor, FormatoImagen formato) {

    public static final String RUTA = "/api/imagenes/";

    private static final Pattern FORMA = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.([a-z]+)");

    public static NombreDeImagen nuevo(FormatoImagen formato) {
        return new NombreDeImagen(UUID.randomUUID() + "." + formato.extension(), formato);
    }

    // Lo que no tiene la forma no es una subida: no existe, en vez de un 400.
    public static Optional<NombreDeImagen> leer(String nombre) {
        Matcher partes = FORMA.matcher(nombre == null ? "" : nombre);
        if (!partes.matches()) {
            return Optional.empty();
        }
        return FormatoImagen.conExtension(partes.group(1)).map(formato -> new NombreDeImagen(nombre, formato));
    }

    public String url() {
        return RUTA + valor;
    }
}
