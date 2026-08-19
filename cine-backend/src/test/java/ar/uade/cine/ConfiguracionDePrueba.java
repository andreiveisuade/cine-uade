package ar.uade.cine;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

/**
 * Lo que el perfil {@code test} pone en lugar de los dos adaptadores que salen del proceso:
 * Redis y TMDB.
 *
 * <p>No se reemplaza la base —esa es H2 de verdad, con el mapeo y las consultas reales— sino
 * lo que no se puede pedir en un test: que Redis esté levantado, y que TMDB conteste sin
 * gastar cuota ni depender de la red.
 */
@Configuration
@Profile("test")
public class ConfiguracionDePrueba {

    /**
     * El reloj de los bloqueos, movible a mano. Es el mismo criterio que
     * {@link ar.uade.cine.model.ventas.Reserva#estaVencida(LocalDateTime)}: probar que algo
     * vence no puede costar esperar a que venza, y una espera real vuelve al test
     * dependiente de lo cargada que esté la máquina.
     */
    public static class Reloj implements Supplier<LocalDateTime> {

        private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 14, 10, 0);

        private final AtomicReference<LocalDateTime> ahora = new AtomicReference<>(INICIO);

        @Override
        public LocalDateTime get() {
            return ahora.get();
        }

        public void mover(LocalDateTime momento) {
            ahora.set(momento);
        }

        public void reiniciar() {
            ahora.set(INICIO);
        }
    }

    @Bean
    public Reloj reloj() {
        return new Reloj();
    }

    @Bean
    public BloqueoButacas bloqueoButacas(Reloj reloj) {
        return new BloqueoButacasMemoria(reloj);
    }

    /** Devuelve el tipo concreto para que un test pueda pedirlo y decirle qué contestar. */
    @Bean
    public CatalogoDePrueba catalogoExterno() {
        return new CatalogoDePrueba();
    }
}
