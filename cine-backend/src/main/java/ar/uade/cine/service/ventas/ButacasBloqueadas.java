package ar.uade.cine.service.ventas;

import java.util.List;

// Resultado de un bloqueo: perder una butaca no es un error, vuelve en rechazadas con el código ya normalizado.
// La sesión, como quedó guardada: es la que el cliente tiene que mandar al reservar.
public record ButacasBloqueadas(String sesion, List<String> conseguidas, List<String> rechazadas) {
}
