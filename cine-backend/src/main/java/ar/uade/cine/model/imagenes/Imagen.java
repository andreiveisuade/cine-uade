package ar.uade.cine.model.imagenes;

import ar.uade.cine.model.rechazos.DatoInvalido;
import lombok.Getter;
import lombok.experimental.Accessors;

// Un archivo de imagen ya revisado: con contenido, de 2 MB como máximo y JPG, PNG o WEBP; Value Object.
@Getter
@Accessors(fluent = true)
public final class Imagen {

    public static final String DEMASIADO_PESADA = "La imagen tiene que pesar 2 MB como máximo";

    private static final int PESO_MAXIMO = 2 * 1024 * 1024;

    private final byte[] contenido;
    private final FormatoImagen formato;

    private Imagen(byte[] contenido, FormatoImagen formato) {
        this.contenido = contenido;
        this.formato = formato;
    }

    public static Imagen subida(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new DatoInvalido("Falta la imagen");
        }
        if (contenido.length > PESO_MAXIMO) {
            throw new DatoInvalido(DEMASIADO_PESADA);
        }
        FormatoImagen formato = FormatoImagen.de(contenido)
                .orElseThrow(() -> new DatoInvalido("La imagen tiene que ser JPG, PNG o WEBP"));
        return new Imagen(contenido, formato);
    }
}
