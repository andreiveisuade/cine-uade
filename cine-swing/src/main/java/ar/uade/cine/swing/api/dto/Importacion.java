package ar.uade.cine.swing.api.dto;

public record Importacion(int id, String estado, int paginas, String pedidaEn, String terminoEn, int nuevas,
                          int salteadas, int fallidas, String detalle) {
}
