package ar.uade.cine.controller.informes;

import org.springframework.boot.context.properties.ConfigurationProperties;

// La sección cine.incaa de application.yml: el encabezado de la declaración jurada.
@ConfigurationProperties("cine.incaa")
public record PropiedadesIncaa(String razonSocial, String cuit, String numeroExhibidor) {
}
