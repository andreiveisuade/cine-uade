package ar.uade.cine.infrastructure;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.PropiedadesComprobantes;
import ar.uade.cine.infrastructure.imagenes.AlmacenDeImagenes;
import ar.uade.cine.infrastructure.imagenes.disco.AlmacenDeImagenesEnDisco;
import ar.uade.cine.infrastructure.imagenes.disco.PropiedadesImagenes;
import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.infrastructure.importador.tmdb.PropiedadesTmdb;
import ar.uade.cine.infrastructure.importador.tmdb.TmdbHttp;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.infrastructure.pasarelas.mercadopago.MercadoPagoHttp;
import ar.uade.cine.infrastructure.pasarelas.mercadopago.PropiedadesMercadoPago;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Elige qué implementa cada puerto (comprobantes, imágenes, pasarela, reloj, TMDB); solo acá se nombran (DIP).
@Configuration
public class Adaptadores {

    @Bean
    public GeneradorTicket generadorTicket(PropiedadesComprobantes comprobantes) {
        return new GeneradorTicketTxt(comprobantes.tickets());
    }

    @Bean
    public GeneradorTicketCandy generadorTicketCandy(PropiedadesComprobantes comprobantes) {
        return new GeneradorTicketCandyTxt(comprobantes.tickets());
    }

    @Bean
    public GeneradorRecibo generadorRecibo(PropiedadesComprobantes comprobantes) {
        return new GeneradorReciboTxt(comprobantes.tickets());
    }

    @Bean
    public AlmacenDeImagenes almacenDeImagenes(PropiedadesImagenes imagenes) {
        return new AlmacenDeImagenesEnDisco(imagenes.dir());
    }

    // Sin tareas de fondo en tests: el RelojMovible mueve el tiempo y una tarea sola borraría filas a mitad de prueba
    @Configuration
    @Profile("!test")
    @EnableScheduling
    static class Tareas {
    }

    @Bean
    @Profile("!test")
    public Reloj reloj() {
        return LocalDateTime::now;
    }

    // Strategy: con token, el Checkout Pro de Mercado Pago (modo prueba no cobra); sin él, la emulada.
    @Bean
    public PasarelaPagos pasarelaPagos(PropiedadesMercadoPago mercadoPago) {
        return mercadoPago.configurado()
                ? new MercadoPagoHttp(mercadoPago.token(), mercadoPago.base(), mercadoPago.sitio())
                : new MercadoPagoEmulado();
    }

    // Sin token el bean se arma igual: no poder importar cartelera no debe impedir vender.
    @Bean
    @Profile("!test")
    public CatalogoExterno catalogoExterno(PropiedadesTmdb tmdb) {
        return new TmdbHttp(tmdb.token(), tmdb.region(), tmdb.base());
    }
}
