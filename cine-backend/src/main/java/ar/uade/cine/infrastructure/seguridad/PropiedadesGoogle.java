package ar.uade.cine.infrastructure.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

// El cliente OAuth de Google Cloud con el que entra el cliente (R28); @ConfigurationProperties de cine.google, opcional.
// Sin client-id no se arma el login con Google: el sistema vende igual y la web avisa que no se puede ingresar.
@ConfigurationProperties("cine.google")
public record PropiedadesGoogle(String clientId, String clientSecret) {

    public boolean configurado() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
