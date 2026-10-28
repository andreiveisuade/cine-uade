package ar.uade.cine.swing.api;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

// Contra qué backend trabaja el panel: local, producción u otro; las URLs conocidas viven solo acá.
public record Servidor(Entorno entorno, String url) {

    public enum Entorno {
        LOCAL("Local", "http://localhost:8080"),
        PRODUCCION("Producción", "https://cine-uade.onrender.com"),
        OTRO("Otro…", null);

        private final String nombre;
        private final String url;

        Entorno(String nombre, String url) {
            this.nombre = nombre;
            this.url = url;
        }

        /** Lo que se ve en el combo del ingreso: «Local — localhost:8080». */
        public String etiqueta() {
            return url == null ? nombre : nombre + " — " + host(url);
        }
    }

    public static final Servidor LOCAL = new Servidor(Entorno.LOCAL, Entorno.LOCAL.url);
    public static final Servidor PRODUCCION = new Servidor(Entorno.PRODUCCION, Entorno.PRODUCCION.url);

    // Render Free apaga la instancia sin tráfico y el primer pedido espera a que arranque.
    private static final Duration ESPERA_DESPERTANDO = Duration.ofSeconds(120);
    private static final Duration ESPERA_DEL_INGRESO = Duration.ofSeconds(30);

    /** Una URL conocida se reconoce aunque venga con barra final o en mayúsculas; cualquier otra es OTRO. */
    public static Servidor de(String url) {
        String limpia = url.trim().replaceAll("/+$", "");
        return Arrays.stream(Entorno.values())
                .filter(e -> e.url != null && e.url.equalsIgnoreCase(limpia))
                .findFirst()
                .map(e -> new Servidor(e, e.url))
                .orElseGet(() -> new Servidor(Entorno.OTRO, limpia));
    }

    public static Servidor de(Entorno entorno) {
        return de(entorno.url);
    }

    /** Propiedad {@code -Dcine.api.url}, si no la variable {@code CINE_API_URL}; mandan sobre lo elegido antes. */
    public static Optional<Servidor> configurado() {
        String propiedad = System.getProperty("cine.api.url");
        if (propiedad != null && !propiedad.isBlank()) return Optional.of(de(propiedad));
        String entorno = System.getenv("CINE_API_URL");
        if (entorno != null && !entorno.isBlank()) return Optional.of(de(entorno));
        return Optional.empty();
    }

    public boolean duerme() {
        return entorno == Entorno.PRODUCCION;
    }

    public Duration esperaDelIngreso() {
        return duerme() ? ESPERA_DESPERTANDO : ESPERA_DEL_INGRESO;
    }

    /** La marca de entorno de la ventana principal. */
    public String marca() {
        return switch (entorno) {
            case LOCAL -> "LOCAL";
            case PRODUCCION -> "PRODUCCIÓN";
            case OTRO -> "OTRO: " + host(url);
        };
    }

    private static String host(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) return url;
            return uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
