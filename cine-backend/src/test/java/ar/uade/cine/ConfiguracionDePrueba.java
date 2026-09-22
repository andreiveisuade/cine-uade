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
 * Lo que el perfil {@code test} pone en lugar de los tres adaptadores que salen del proceso:
 * Redis, TMDB y el reloj de la máquina.
 *
 * <p>No se reemplaza la base —esa es H2 de verdad, con el mapeo y las consultas reales— sino
 * lo que no se puede pedir en un test: que Redis esté levantado, que TMDB conteste sin
 * gastar cuota ni depender de la red, y que hoy sea siempre el mismo día.
 */
@Configuration
@Profile("test")
public class ConfiguracionDePrueba {

    /**
     * El reloj de todo el sistema bajo prueba, movible a mano. Arranca siempre el 14 de
     * agosto de 2026 a las 10: las fechas escritas en los tests —la función del 20 a las
     * 20:00, la promo de todo el año— son futuro respecto de ese instante, y lo siguen
     * siendo el día que el calendario de la máquina las pase. Es el mismo criterio que
     * {@link ar.uade.cine.model.ventas.Reserva#estaVencida(LocalDateTime)}: probar que algo
     * vence no puede costar esperar a que venza, y una espera real vuelve al test
     * dependiente de lo cargada que esté la máquina.
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
