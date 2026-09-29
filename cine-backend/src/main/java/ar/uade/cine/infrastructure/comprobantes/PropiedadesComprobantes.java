package ar.uade.cine.infrastructure.comprobantes;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// La sección cine.comprobantes de application.yml: dónde escriben los tres generadores.
@ConfigurationProperties("cine.comprobantes")
public record PropiedadesComprobantes(Path tickets) {
}
