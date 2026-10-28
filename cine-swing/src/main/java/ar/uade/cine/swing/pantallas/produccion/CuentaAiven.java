package ar.uade.cine.swing.pantallas.produccion;

import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

// Token, proyecto y servicio de Aiven: del sistema o de las preferencias de esta compu, nunca del repo (es público).
public record CuentaAiven(String token, String proyecto, String servicio, boolean tokenDelSistema) {

    static final String TOKEN = "aiven.token";
    static final String PROYECTO = "aiven.proyecto";
    static final String SERVICIO = "aiven.servicio";

    /** {@code AIVEN_TOKEN} → {@code -Dcine.aiven.token}, si no la variable de entorno. */
    public static final UnaryOperator<String> DEL_SISTEMA = nombre -> {
        String propiedad = System.getProperty("cine." + nombre.toLowerCase(Locale.ROOT).replace('_', '.'));
        return propiedad != null ? propiedad : System.getenv(nombre);
    };

    static CuentaAiven leer(Preferences preferencias, UnaryOperator<String> sistema) {
        String token = limpio(sistema.apply("AIVEN_TOKEN"));
        return new CuentaAiven(token != null ? token : limpio(preferencias.get(TOKEN, null)),
                primero(sistema.apply("AIVEN_PROYECTO"), preferencias.get(PROYECTO, null)),
                primero(sistema.apply("AIVEN_SERVICIO"), preferencias.get(SERVICIO, null)), token != null);
    }

    /** Un token vacío no pisa el guardado: el diálogo deja el campo en blanco para no mostrarlo. */
    static void guardar(Preferences preferencias, String proyecto, String servicio, String token) {
        preferencias.put(PROYECTO, proyecto.trim());
        preferencias.put(SERVICIO, servicio.trim());
        if (limpio(token) != null) preferencias.put(TOKEN, token.trim());
    }

    static void olvidarToken(Preferences preferencias) {
        preferencias.remove(TOKEN);
    }

    public boolean conToken() {
        return token != null;
    }

    public boolean completa() {
        return token != null && proyecto != null && servicio != null;
    }

    private static String primero(String delSistema, String guardado) {
        String valor = limpio(delSistema);
        return valor != null ? valor : limpio(guardado);
    }

    private static String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    // El token no sale en un log ni en un mensaje de error.
    @Override
    public String toString() {
        return "CuentaAiven[proyecto=" + proyecto + ", servicio=" + servicio
                + ", token=" + (conToken() ? "***" : "no") + "]";
    }
}
