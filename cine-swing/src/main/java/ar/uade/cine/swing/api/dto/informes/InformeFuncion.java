package ar.uade.cine.swing.api.dto.informes;

// Lo que dejó una función entre boletería y candy: su borderó más el candy vendido con reserva.
public record InformeFuncion(Bordero boleteria, int comprasCandy, double candy, double total) {
}
