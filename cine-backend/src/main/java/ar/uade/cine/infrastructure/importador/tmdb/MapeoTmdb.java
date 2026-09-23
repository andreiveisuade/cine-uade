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

/**
 * Traduce el modelo de TMDB al nuestro en un solo lugar, para que no se filtre al resto.
 * Las tablas apuntan a {@link Genero} y {@link Clasificacion}: renombrar una constante no
 * compila en vez de romper en la próxima importación.
 */
final class MapeoTmdb {

    /** Los 19 géneros de TMDB caen en el más cercano de los 9 nuestros: pérdida asumida. */
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

    /** TMDB trae la certificación del INCAA en varias formas: +13, 13, SAM13. */
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
            // Condicionada: es la más restrictiva que publica el INCAA.
            entry("C", Clasificacion.MAS_18));

    private static final Map<String, String> IDIOMAS = Map.ofEntries(
            entry("en", "Inglés"), entry("es", "Español"), entry("fr", "Francés"),
            entry("it", "Italiano"), entry("pt", "Portugués"), entry("de", "Alemán"),
            entry("ja", "Japonés"), entry("ko", "Coreano"), entry("zh", "Chino"),
            entry("cn", "Chino"), entry("ru", "Ruso"), entry("hi", "Hindi"));

    private MapeoTmdb() {
    }

    /**
     * Nace {@code enCartelera = false} para que nada se publique sin que el encargado la mire.
     *
     * @param certificacion la argentina, o {@code null} si TMDB no la trae
     */
    static DatosPelicula aPelicula(JsonNode resumen, JsonNode detalle, String certificacion,
                                   String urlPoster) {
        return new DatosPelicula(
                textoDe(detalle, "title", textoDe(resumen, "title", "")),
                detalle.path("runtime").asInt(0),
                generosDe(detalle),
                clasificacionDe(certificacion),
                // Vendría de /credits, una llamada más; lo completa el encargado.
                "",
                textoDe(detalle, "overview", ""),
                anioDe(detalle.path("release_date").asText(null)),
                idiomaDe(detalle.path("original_language").asText("")),
                urlPoster,
                false,
                // 0..10; el rango lo valida el gestor.
                detalle.path("vote_average").asDouble(0),
                // Sin la cantidad de votos, un 0,0 recién estrenado parece una película mala.
                detalle.path("vote_count").asInt(0));
    }

    /** Al menos uno (R7): si TMDB no trae ninguno reconocible, DRAMA. */
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

    /** Sin certificación, MAS_13 y no ATP: el default prudente; el encargado lo corrige. */
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

    private static String textoDe(JsonNode nodo, String campo, String siNoEsta) {
        String valor = nodo.path(campo).asText("");
        return valor.isBlank() ? siNoEsta : valor.strip();
    }
}
