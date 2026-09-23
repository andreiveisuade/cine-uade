package ar.uade.cine.controller.http;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** El formato de fechas del contrato HTTP, en un solo lugar para que ninguna vista se desvíe. */
public final class Fechas {

    /** ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Fechas() {
    }

    public static String texto(LocalDateTime momento) {
        return momento == null ? null : momento.format(ISO);
    }
}
