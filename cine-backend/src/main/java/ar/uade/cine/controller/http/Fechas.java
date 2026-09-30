package ar.uade.cine.controller.http;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Formatea fechas al ISO local sin zona del contrato (API.md); un solo formato para todas las Vistas.
public final class Fechas {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Fechas() {
    }

    public static String texto(LocalDateTime momento) {
        return momento == null ? null : momento.format(ISO);
    }
}
