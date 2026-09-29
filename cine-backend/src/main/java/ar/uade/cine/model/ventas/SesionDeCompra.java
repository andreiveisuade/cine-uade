package ar.uade.cine.model.ventas;

import ar.uade.cine.model.validacion.Regla;

// La sesión del navegador que está eligiendo butacas; Value Object presente, recortado y del largo de la columna.
// No es una credencial: dice de quién es un bloqueo, y la doble venta la sigue impidiendo el UNIQUE de entrada.
public record SesionDeCompra(String valor) {

    // El largo de bloqueo_butaca.sesion. El front manda un UUID, que mide 36.
    private static final int LARGO_MAXIMO = 64;

    public SesionDeCompra {
        valor = Regla.texto(valor).obligatorio("Falta la sesión para bloquear butacas").recortado()
                .hasta(LARGO_MAXIMO, "La sesión").valor();
    }

    // Al reservar y en el mapa la sesión es optativa: sin ella no hay bloqueos propios que descontar. Se
    // recorta como al bloquear, o " abc" no reconocería como suyos los bloqueos que tomó "abc".
    public static String comoSeGuarda(String sesion) {
        return sesion == null ? null : sesion.strip();
    }
}
