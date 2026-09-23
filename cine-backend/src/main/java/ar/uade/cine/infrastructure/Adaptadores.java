package ar.uade.cine.infrastructure;

import java.nio.file.Path;
import java.time.LocalDateTime;

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
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * Elige la implementación de cada puerto hacia afuera: es el único lugar que las nombra.
 * Van como {@code @Bean} y no {@code @Component} porque dependen de {@code application.yml}
 * y la decisión se lee mejor junta. Los comprobantes van a disco: se entregan, no se consultan.
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
     * Redis y no la base: los bloqueos duran minutos y después no importan. Si Redis cae,
     * se vende sin bloqueos y la doble venta la sigue impidiendo el UNIQUE de la base.
     * El perfil de test usa la implementación en memoria.
     */
    @Bean
    @Profile("!test")
    public BloqueoButacas bloqueoButacas(@Value("${cine.redis.host}") String host,
                                         @Value("${cine.redis.puerto}") int puerto) {
        return new BloqueoButacasRedis(host, puerto);
    }

    /** El perfil de test pone un reloj que se mueve a mano. */
    @Bean
    @Profile("!test")
    public Reloj reloj() {
        return LocalDateTime::now;
    }

    @Bean
    public PasarelaPagos pasarelaPagos() {
        return new MercadoPagoEmulado();
    }

    /**
     * Sin token el bean se arma igual: no poder importar cartelera no debe impedir vender.
     * La pantalla del importador avisa que falta.
     */
    @Bean
    @Profile("!test")
    public CatalogoExterno catalogoExterno(@Value("${cine.tmdb.token}") String token,
                                           @Value("${cine.tmdb.region}") String region,
                                           @Value("${cine.tmdb.base}") String base) {
        return new TmdbHttp(token, region, base);
    }
}
