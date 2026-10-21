package ar.uade.cine.model.imagenes;

import ar.uade.cine.model.rechazos.DatoInvalido;

// La imagen de una promoción o un producto: vacía, o una subida por POST /api/imagenes; Value Object.
// Solo propias: una URL ajena podría dejar de existir o rastrear a quien mira la carta.
public record ImagenUrl(String valor) {

    public static final ImagenUrl NINGUNA = new ImagenUrl(null);

    public ImagenUrl {
        valor = valor == null || valor.isBlank() ? null : valor.trim();
        if (valor != null && !esSubida(valor)) {
            throw new DatoInvalido("La imagen tiene que ser una subida al sistema");
        }
    }

    private static boolean esSubida(String url) {
        return url.startsWith(NombreDeImagen.RUTA)
                && NombreDeImagen.leer(url.substring(NombreDeImagen.RUTA.length())).isPresent();
    }
}
