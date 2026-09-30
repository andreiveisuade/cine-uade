package ar.uade.cine.model.ventas.validacion;

import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.validacion.Regla;

// Qué trae una entrada al venderse: una butaca en servicio (R9); guardas de Regla que llama Entrada.
// Que la butaca esté libre no va acá: depende de las otras reservas, y lo mira Ocupacion.
public final class ValidadorEntrada {

    private ValidadorEntrada() {
    }

    public static void validar(Asiento asiento) {
        Regla.objeto(asiento).obligatorio("Falta la butaca de la entrada").valor().exigirEnServicio();
    }
}
