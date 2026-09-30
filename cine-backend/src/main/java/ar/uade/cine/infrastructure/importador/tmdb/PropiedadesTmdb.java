package ar.uade.cine.infrastructure.importador.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Token, región y URL base de TMDB; @ConfigurationProperties de cine.tmdb, token opcional (ver Adaptadores).
@ConfigurationProperties("cine.tmdb")
public record PropiedadesTmdb(String token, String region, String base) {
}
