package ar.uade.cine.controller.informes;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Datos del exhibidor para el encabezado de la declaración jurada; @ConfigurationProperties de cine.incaa.
@ConfigurationProperties("cine.incaa")
public record PropiedadesIncaa(String razonSocial, String cuit, String numeroExhibidor) {
}
