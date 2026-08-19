package ar.uade.cine.infrastructure;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ar.uade.cine.infrastructure.comprobantes.GeneradorBordero;
import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasRedis;
import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.infrastructure.importador.tmdb.TmdbHttp;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;

/**
 * Los puertos hacia afuera, elegidos y configurados: dónde caen los comprobantes, quién
 * cobra y de dónde salen las películas que el cine no cargó a mano.
 *
 * <p>Es a {@code infraestructura/} lo que {@link ar.uade.cine.repository.PersistenciaMySQL}
 * es a {@code persistencia/}: el único lugar que nombra una implementación concreta. Y por
 * el mismo motivo se declaran con {@code @Bean} en vez de anotarse con {@code @Component}
 * —los tres necesitan configuración que sale de {@code application.yml}, así que hay algo
 * que decidir, y esa decisión se lee mejor junta que repartida en anotaciones.
 *
 * <p>Los tickets van a disco y no a la base a propósito. Un comprobante es un papel que se
 * entrega, no un dato que se consulta: por eso su contrato es {@link GeneradorTicket} y no
 * un DAO.
 *
 * <p>{@link MercadoPagoEmulado} es la única implementación de pasarela que hay, y este es
 * el único lugar que la nombra: enchufar la integración de verdad —con credenciales y
 * llamadas de red— es cambiar este método, sin tocar una regla de negocio.
 *
 * <p>{@link TmdbHttp} es lo mismo del otro lado: de dónde salen las películas importadas es
 * una decisión de armado, y este es el único lugar que nombra a TMDB. Un test le pasa otro
 * catálogo y prueba el circuito entero sin gastar cuota.
 */
@Configuration
public class Adaptadores {

    @Bean
    public GeneradorTicket generadorTicket(@Value("${cine.comprobantes.tickets}") Path directorio) {
        return new GeneradorTicketTxt(directorio);
    }

    @Bean
    public GeneradorTicketCandy generadorTicketCandy(
            @Value("${cine.comprobantes.tickets}") Path directorio) {
        return new GeneradorTicketCandyTxt(directorio);
    }

    @Bean
    public GeneradorRecibo generadorRecibo(@Value("${cine.comprobantes.tickets}") Path directorio) {
        return new GeneradorReciboTxt(directorio);
    }

    @Bean
    public GeneradorBordero generadorBordero(@Value("${cine.comprobantes.informes}") Path directorio) {
        return new GeneradorBorderoTxt(directorio);
    }

    /**
     * Los bloqueos van a Redis y no a la base porque son lo contrario de todo lo demás que
     * se guarda: duran tres minutos y después no le importan a nadie. Si Redis no está,
     * {@link BloqueoButacasRedis} degrada a "ningún bloqueo" y el sistema sigue vendiendo
     * como antes de que existiera —la doble venta la sigue impidiendo el UNIQUE de la base.
     *
     * <p>Fuera del perfil de test, que usa la implementación en memoria con un reloj que se
     * puede mover a mano: probar que un bloqueo vence no puede costar tres minutos de espera.
     */
    @Bean
    @Profile("!test")
    public BloqueoButacas bloqueoButacas(@Value("${cine.redis.host}") String host,
                                         @Value("${cine.redis.puerto}") int puerto) {
        return new BloqueoButacasRedis(host, puerto);
    }

    @Bean
    public PasarelaPagos pasarelaPagos() {
        return new MercadoPagoEmulado();
    }

    /**
     * Sin token el bean se arma igual, con la cadena vacía: el sistema tiene que levantar
     * aunque nadie haya sacado su credencial de TMDB, y la pantalla del importador es la
     * que avisa que falta. Que la aplicación no arranque por esto sería impedir vender
     * entradas por no poder importar cartelera.
     */
    @Bean
    @Profile("!test")
    public CatalogoExterno catalogoExterno(@Value("${cine.tmdb.token}") String token,
                                           @Value("${cine.tmdb.region}") String region,
                                           @Value("${cine.tmdb.base}") String base) {
        return new TmdbHttp(token, region, base);
    }
}
