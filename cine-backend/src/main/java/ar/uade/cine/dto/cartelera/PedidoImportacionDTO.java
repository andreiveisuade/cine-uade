package ar.uade.cine.dto.cartelera;

// El cuerpo opcional de POST /api/importaciones; sin paginas el gestor trae una, fuera de 1 a 3 rechaza.
public record PedidoImportacionDTO(Integer paginas) {
}
