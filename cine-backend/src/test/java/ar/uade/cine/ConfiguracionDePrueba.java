package ar.uade.cine;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * Reemplaza en el perfil {@code test} lo que sale del proceso: Redis, TMDB y el reloj.
 * La base no se reemplaza: H2 corre el mapeo y las consultas reales.
 */
@Configuration
@Profile("test")
public class ConfiguracionDePrueba {

    /**
     * Arranca fijo el 14/08/2026 a las 10 para que las fechas de los tests sigan siendo
     * futuro cuando el calendario las pase, y para probar vencimientos sin esperar.
     */
    public static class RelojMovible implements Reloj {

        public static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 14, 10, 0);

        private final AtomicReference<LocalDateTime> ahora = new AtomicReference<>(INICIO);

        @Override
        public LocalDateTime ahora() {
            return ahora.get();
        }

        public void mover(LocalDateTime momento) {
            ahora.set(momento);
        }

        public void reiniciar() {
            ahora.set(INICIO);
        }
    }

    /** Devuelve el tipo concreto para que un test pueda pedirlo y moverlo. */
    @Bean
    public RelojMovible reloj() {
        return new RelojMovible();
    }

    @Bean
    public BloqueoButacas bloqueoButacas(RelojMovible reloj) {
        return new BloqueoButacasMemoria(reloj);
    }

    /** Devuelve el tipo concreto para que un test pueda pedirlo y decirle qué contestar. */
    @Bean
    public CatalogoDePrueba catalogoExterno() {
        return new CatalogoDePrueba();
    }
}
