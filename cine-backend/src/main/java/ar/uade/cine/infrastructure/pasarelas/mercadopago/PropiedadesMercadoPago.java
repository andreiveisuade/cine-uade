package ar.uade.cine.infrastructure.pasarelas.mercadopago;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Token, URL de la API y URL pública del sitio; @ConfigurationProperties de cine.mercadopago, token opcional.
// El sitio es adonde Mercado Pago devuelve al comprador: el navegador, no el contenedor, tiene que llegar.
@ConfigurationProperties("cine.mercadopago")
public record PropiedadesMercadoPago(String token, String base, String sitio) {

    public boolean configurado() {
        return token != null && !token.isBlank();
    }
}
