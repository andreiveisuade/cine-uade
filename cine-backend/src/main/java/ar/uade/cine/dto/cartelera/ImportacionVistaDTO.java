package ar.uade.cine.dto.cartelera;

public record ImportacionVistaDTO(int id, String estado, int paginas, String pedidaEn,
                                  String terminoEn, int nuevas, int salteadas, int fallidas,
                                  String detalle) {
}
