package ar.uade.cine.swing.api.dto.funciones;

// Una función que el borrado en lote no borró, con el mensaje del backend (R12, o que ya no existe).
public record FuncionRechazada(int id, String error) {
}
