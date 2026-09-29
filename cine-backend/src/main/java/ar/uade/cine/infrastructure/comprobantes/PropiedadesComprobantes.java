package ar.uade.cine.infrastructure.comprobantes;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Carpeta donde se escriben los comprobantes .txt; @ConfigurationProperties de cine.comprobantes.
@ConfigurationProperties("cine.comprobantes")
public record PropiedadesComprobantes(Path tickets) {
}
