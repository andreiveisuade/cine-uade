package ar.uade.cine.dto.informes;

// El encabezado de la declaración jurada; sale de la configuración cine.incaa.* y no de la base.
public record ExhibidorVistaDTO(String razonSocial, String cuit, String numeroExhibidor) {
}
