package ar.uade.cine.infrastructure.imagenes.disco;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Carpeta donde se guardan las imágenes subidas; @ConfigurationProperties de cine.imagenes.
@ConfigurationProperties("cine.imagenes")
public record PropiedadesImagenes(Path dir) {
}
