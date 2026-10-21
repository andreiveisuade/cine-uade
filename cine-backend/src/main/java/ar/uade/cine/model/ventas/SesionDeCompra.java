package ar.uade.cine.model.ventas;

import ar.uade.cine.model.validacion.Regla;

// Sesión del navegador que elige butacas; Value Object presente, recortado y del largo de la columna.
// No es una credencial: dice de quién es un bloqueo.
public record SesionDeCompra(String valor) {

    // El largo de bloqueo_butaca.sesion. El front manda un UUID, que mide 36.
    private static final int LARGO_MAXIMO = 64;

    public SesionDeCompra {
        valor = Regla.texto(valor).obligatorio("Falta la sesión para bloquear butacas").recortado()
                .hasta(LARGO_MAXIMO, "La sesión").valor();
    }

    // Optativa al reservar y en el mapa; se recorta como al bloquear, o " abc" no reconocería los bloqueos de "abc".
    public static String comoSeGuarda(String sesion) {
        return sesion == null ? null : sesion.strip();
    }
}
