package ar.uade.cine.infrastructure.importador.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

// La sección cine.tmdb de application.yml. El token puede venir vacío: ver Adaptadores.
@ConfigurationProperties("cine.tmdb")
public record PropiedadesTmdb(String token, String region, String base) {
}
