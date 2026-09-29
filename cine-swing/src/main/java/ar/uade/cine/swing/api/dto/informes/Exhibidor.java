package ar.uade.cine.swing.api.dto.informes;

// El encabezado de la declaración jurada: razón social, CUIT y número de exhibidor ante el INCAA.
public record Exhibidor(String razonSocial, String cuit, String numeroExhibidor) {
}
