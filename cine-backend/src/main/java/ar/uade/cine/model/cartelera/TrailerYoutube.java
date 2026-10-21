package ar.uade.cine.model.cartelera;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Trailer de una película: el id de 11 caracteres de un video de YouTube; Value Object.
// Acepta también la URL que se copia del navegador (watch?v=, youtu.be/, /embed/) y se queda con el id.
public record TrailerYoutube(String id) {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private static final Pattern URL = Pattern.compile("(?:https?://)?"
            + "(?:(?:www\\.|m\\.)?youtube\\.com/(?:watch\\?(?:[^#]*&)?v=|embed/)|youtu\\.be/)"
            + "([A-Za-z0-9_-]{11})(?:[?&#].*)?");

    public TrailerYoutube {
        String texto = id == null ? "" : id.strip();
        Matcher url = URL.matcher(texto);
        if (url.matches()) {
            texto = url.group(1);
        }
        if (!ID.matcher(texto).matches()) {
            throw new DatoInvalido("El trailer tiene que ser un video de YouTube");
        }
        id = texto;
    }

    // Para lo que trae TMDB: un video raro deja la película sin trailer en vez de rechazarla entera.
    public static Optional<TrailerYoutube> siEs(String texto) {
        try {
            return Optional.of(new TrailerYoutube(texto));
        } catch (DatoInvalido noEsUnVideo) {
            return Optional.empty();
        }
    }
}
