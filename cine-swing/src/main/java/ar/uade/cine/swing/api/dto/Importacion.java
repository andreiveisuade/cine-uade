package ar.uade.cine.swing.api.dto;

public record Importacion(int id, String estado, String pedidaEn, int nuevas,
                          int salteadas, int fallidas, String detalle) {
}
