package ar.uade.cine.infrastructure.importador.tmdb;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

// Traduce el JSON de TMDB a DatosPelicula (géneros, certificación, idioma, póster); Fabricación pura.
final class MapeoTmdb {

    private static final String IMAGENES = "https://image.tmdb.org/t/p/w500";

    private static final Map<String, Genero> GENEROS = Map.ofEntries(
            entry("Acción", Genero.ACCION),
            entry("Aventura", Genero.ACCION),
            entry("Bélica", Genero.ACCION),
            entry("Western", Genero.ACCION),
            entry("Comedia", Genero.COMEDIA),
            entry("Drama", Genero.DRAMA),
            entry("Historia", Genero.DRAMA),
            entry("Música", Genero.DRAMA),
            entry("Película de TV", Genero.DRAMA),
            entry("Terror", Genero.TERROR),
            entry("Ciencia ficción", Genero.CIENCIA_FICCION),
            entry("Fantasía", Genero.CIENCIA_FICCION),
            entry("Animación", Genero.ANIMACION),
            entry("Familia", Genero.ANIMACION),
            entry("Documental", Genero.DOCUMENTAL),
            entry("Romance", Genero.ROMANCE),
            entry("Suspense", Genero.SUSPENSO),
            entry("Misterio", Genero.SUSPENSO),
            entry("Thriller", Genero.SUSPENSO),
            entry("Crimen", Genero.SUSPENSO));

    private static final Map<String, Clasificacion> CLASIFICACIONES = Map.ofEntries(
            entry("ATP", Clasificacion.ATP),
            entry("+13", Clasificacion.MAS_13),
            entry("13", Clasificacion.MAS_13),
            entry("SAM13", Clasificacion.MAS_13),
            entry("+16", Clasificacion.MAS_16),
            entry("16", Clasificacion.MAS_16),
            entry("SAM16", Clasificacion.MAS_16),
            entry("+18", Clasificacion.MAS_18),
            entry("18", Clasificacion.MAS_18),
            entry("SAM18", Clasificacion.MAS_18),
            entry("C", Clasificacion.MAS_18));

    private static final Map<String, String> IDIOMAS = Map.ofEntries(
            entry("en", "Inglés"), entry("es", "Español"), entry("fr", "Francés"),
            entry("it", "Italiano"), entry("pt", "Portugués"), entry("de", "Alemán"),
            entry("ja", "Japonés"), entry("ko", "Coreano"), entry("zh", "Chino"),
            entry("cn", "Chino"), entry("ru", "Ruso"), entry("hi", "Hindi"));

    private MapeoTmdb() {
    }

    // Las tres respuestas de TMDB por película: la del listado, el detalle y las fechas de estreno.
    // Nace enCartelera = false: nada se publica sin que el encargado la mire.
    static DatosPelicula aPelicula(JsonNode resumen, JsonNode detalle, JsonNode estrenos) {
        return new DatosPelicula(
                textoDe(detalle, "title", textoDe(resumen, "title", "")),
                detalle.path("runtime").asInt(0),
                generosDe(detalle),
                clasificacionDe(certificacionArgentina(estrenos)),
                "",
                textoDe(detalle, "overview", ""),
                anioDe(detalle.path("release_date").asText(null)),
                idiomaDe(detalle.path("original_language").asText("")),
                urlPoster(resumen.path("poster_path").asText(null)),
                false,
                puntajeDe(detalle.path("vote_average").asDouble(0)),
                detalle.path("vote_count").asInt(0));
    }

    static List<Genero> generosDe(JsonNode detalle) {
        // TreeSet: orden estable entre corridas de la misma película.
        TreeSet<Genero> traducidos = new TreeSet<>();
        for (JsonNode genero : detalle.path("genres")) {
            Genero nuestro = GENEROS.get(genero.path("name").asText(""));
            if (nuestro != null) {
                traducidos.add(nuestro);
            }
        }
        return traducidos.isEmpty() ? List.of(Genero.DRAMA) : new ArrayList<>(traducidos);
    }

    // La primera no vacía de los estrenos en la Argentina; null si no hay, y la clasificación decide.
    static String certificacionArgentina(JsonNode estrenos) {
        for (JsonNode pais : estrenos.path("results")) {
            if (!"AR".equals(pais.path("iso_3166_1").asText())) {
                continue;
            }
            for (JsonNode estreno : pais.path("release_dates")) {
                String certificacion = estreno.path("certification").asText("");
                if (!certificacion.isBlank()) {
                    return certificacion.strip();
                }
            }
        }
        return null;
    }

    // Sin certificación, MAS_13 y no ATP: el default prudente.
    static Clasificacion clasificacionDe(String certificacion) {
        if (certificacion == null || certificacion.isBlank()) {
            return Clasificacion.MAS_13;
        }
        return CLASIFICACIONES.getOrDefault(
                certificacion.strip().toUpperCase(), Clasificacion.MAS_13);
    }

    static int anioDe(String fechaEstreno) {
        if (fechaEstreno == null || fechaEstreno.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(fechaEstreno.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String idiomaDe(String codigo) {
        return IDIOMAS.getOrDefault(codigo, codigo);
    }

    // TMDB lo manda con tres decimales (7.234) y la película acepta uno, el de la columna DECIMAL(3,1).
    static double puntajeDe(double promedio) {
        return Math.round(promedio * 10) / 10.0;
    }

    static String urlPoster(String posterPath) {
        return posterPath == null || posterPath.isBlank() ? "" : IMAGENES + posterPath;
    }

    private static String textoDe(JsonNode nodo, String campo, String siNoEsta) {
        String valor = nodo.path(campo).asText("");
        return valor.isBlank() ? siNoEsta : valor.strip();
    }
}
