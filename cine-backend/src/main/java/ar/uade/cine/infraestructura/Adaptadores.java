package ar.uade.cine.infraestructura;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ar.uade.cine.infraestructura.comprobantes.GeneradorBordero;
import ar.uade.cine.infraestructura.comprobantes.GeneradorRecibo;
import ar.uade.cine.infraestructura.comprobantes.GeneradorTicket;
import ar.uade.cine.infraestructura.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infraestructura.importador.CatalogoExterno;
import ar.uade.cine.infraestructura.importador.tmdb.TmdbHttp;
import ar.uade.cine.infraestructura.pasarelas.PasarelaPagos;
import ar.uade.cine.infraestructura.pasarelas.emulada.MercadoPagoEmulado;

/**
 * Los puertos hacia afuera, elegidos y configurados: dónde caen los comprobantes, quién
 * cobra y de dónde salen las películas que el cine no cargó a mano.
 *
 * <p>Es a {@code infraestructura/} lo que {@link ar.uade.cine.persistencia.PersistenciaMySQL}
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
    public CatalogoExterno catalogoExterno(@Value("${cine.tmdb.token}") String token,
                                           @Value("${cine.tmdb.region}") String region,
                                           @Value("${cine.tmdb.base}") String base) {
        return new TmdbHttp(token, region, base);
    }
}
