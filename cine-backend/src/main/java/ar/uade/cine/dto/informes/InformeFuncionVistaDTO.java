package ar.uade.cine.dto.informes;

// La recaudación de una función (GET /api/funciones/{id}/informe): su borderó más el candy con reserva.
public record InformeFuncionVistaDTO(BorderoVistaDTO boleteria, int comprasCandy, double candy,
                                  double total) {
}
