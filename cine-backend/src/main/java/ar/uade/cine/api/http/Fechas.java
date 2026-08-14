package ar.uade.cine.api.http;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * El formato en que viajan las fechas por HTTP. Está acá y no en cada vista porque el
 * contrato es uno solo: si una respuesta usara otro formato, el front lo parsearía mal
 * en esa pantalla y en ninguna otra, que es la clase de error que aparece tarde.
 */
public final class Fechas {

    /** El contrato pide ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Fechas() {
    }

    /** {@code null} viaja como null: es "todavía no pasó", no una fecha vacía. */
    public static String texto(LocalDateTime momento) {
        return momento == null ? null : momento.format(ISO);
    }
}
