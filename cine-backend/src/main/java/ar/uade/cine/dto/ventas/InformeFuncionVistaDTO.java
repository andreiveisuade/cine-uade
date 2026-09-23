package ar.uade.cine.dto.ventas;

/**
 * {@code boleteria} es el mismo borderó de su endpoint, para que los dos informes no
 * difieran. {@code candy} es solo el atribuible a la función; el de mostrador va al arqueo.
 */
public record InformeFuncionVistaDTO(BorderoVistaDTO boleteria, int comprasCandy, double candy,
                                  double total) {
}
