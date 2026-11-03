package ar.uade.cine.dto.funciones;

// Una función que el borrado en lote no borró, en BorradoVistaDTO; error, el mismo que da el DELETE.
public record FuncionRechazadaVistaDTO(int id, String error) {
}
