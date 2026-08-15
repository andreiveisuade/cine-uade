package ar.uade.cine.infraestructura.importador.tmdb;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.servicio.cartelera.DatosPelicula;

/**
 * Traduce el modelo de TMDB al nuestro.
 *
 * <p>Es el corazón del importador y vale la pena entender por qué existe: dos sistemas que
 * hablan del mismo dominio casi nunca lo modelan igual. TMDB tiene 19 géneros, nosotros 9;
 * ellos guardan el idioma como código ISO 639 (<code>en</code>), nosotros como texto
 * (<code>Inglés</code>); su certificación es un par (país, valor) y hay que buscar la
 * argentina entre todas, la nuestra es un enum de cuatro valores.
 *
 * <p>Traducir en un solo lugar es lo que evita que esas diferencias se filtren al resto del
 * sistema. De acá para adentro TMDB no existe.
 *
 * <p>Las tablas son contra los enums de {@code dominio} y no contra texto, que es la razón
 * de que esto sea Java y no el {@code mapeo.py} que era antes: renombrar una constante de
 * {@link Genero} o de {@link Clasificacion} ahora <strong>no compila</strong>. Cuando la
 * traducción vivía afuera, el mismo cambio pasaba todos los tests y rompía recién en la
 * corrida siguiente, contra el buzón.
 */
final class MapeoTmdb {

    /**
     * Los 19 géneros de TMDB contra los 9 nuestros, por el nombre que TMDB devuelve en
     * español. Los que no tienen equivalente caen en el más cercano, y eso es una pérdida de
     * información asumida: preferimos un catálogo corto y cerrado —que hace imposible un alta
     * con un género mal tipeado— a copiar el de ellos.
     */
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

    /**
     * Lo que publica el INCAA contra nuestro enum. TMDB devuelve el texto tal cual figura en
     * la certificación argentina, y no siempre con la misma forma: hay títulos con
     * <code>+13</code>, otros con <code>13</code> y otros con <code>SAM13</code>.
     */
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
     * La película como la espera el alta, con los nombres nuestros.
     *
     * <p>Nace {@code enCartelera = false}: lo que baja de TMDB es una propuesta y el buzón la
     * pone en pendiente igual, pero mandarla ya apagada evita que un cambio en el buzón la
     * publique sin que nadie la mire.
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
                // Vendría de /credits: una llamada más por película, y el encargado lo
                // completa a mano cuando confirma.
                "",
                textoDe(detalle, "overview", ""),
                anioDe(detalle.path("release_date").asText(null)),
                idiomaDe(detalle.path("original_language").asText("")),
                urlPoster,
                false,
                // El puntaje de TMDB, que el cine usa para decidir qué programa. Viene 0..10
                // con decimales; se manda tal cual y el gestor valida el rango.
                detalle.path("vote_average").asDouble(0),
                // Sobre cuántos votos se calculó ese puntaje. Sin esto, el 0,0 de una película
                // recién estrenada es indistinguible del de una mala, y seis votos pesan igual
                // que cinco mil.
                detalle.path("vote_count").asInt(0));
    }

    /**
     * Al menos uno, siempre: R7 lo exige y el alta lo rechazaría. Si TMDB no trae ninguno
     * reconocible, DRAMA es el menos comprometido.
     */
    static List<Genero> generosDe(JsonNode detalle) {
        // TreeSet y no un Set cualquiera: sin orden estable, dos corridas de la misma película
        // guardarían los mismos géneros en distinto orden y el diff del buzón sería ruido.
        TreeSet<Genero> traducidos = new TreeSet<>();
        for (JsonNode genero : detalle.path("genres")) {
            Genero nuestro = GENEROS.get(genero.path("name").asText(""));
            if (nuestro != null) {
                traducidos.add(nuestro);
            }
        }
        return traducidos.isEmpty() ? List.of(Genero.DRAMA) : new ArrayList<>(traducidos);
    }

    /**
     * Sin certificación argentina no inventamos una permisiva: ATP dejaría entrar a un menor
     * a cualquier cosa. MAS_13 es el default prudente, y el encargado lo corrige al confirmar.
     */
    static Clasificacion clasificacionDe(String certificacion) {
        if (certificacion == null || certificacion.isBlank()) {
            return Clasificacion.MAS_13;
        }
        return CLASIFICACIONES.getOrDefault(
                certificacion.strip().toUpperCase(), Clasificacion.MAS_13);
    }

    /** El año del estreno, que TMDB manda como fecha ISO. Cero si no vino o no se entiende. */
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

    /** El código ISO 639 traducido. Si no está en la tabla se muestra el código igual. */
    static String idiomaDe(String codigo) {
        return IDIOMAS.getOrDefault(codigo, codigo);
    }

    private static String textoDe(JsonNode nodo, String campo, String siNoEsta) {
        String valor = nodo.path(campo).asText("");
        return valor.isBlank() ? siNoEsta : valor.strip();
    }
}
