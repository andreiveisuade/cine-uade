package ar.uade.cine.service.cartelera;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// La sección cine.importador de application.yml. Spring convierte "5m" o "60s" a Duration.
@ConfigurationProperties("cine.importador")
public record PropiedadesImportador(Duration corridaMaxima, Duration esperaEntreCorridas) {
}
