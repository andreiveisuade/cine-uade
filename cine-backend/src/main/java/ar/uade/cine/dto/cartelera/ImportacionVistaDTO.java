package ar.uade.cine.dto.cartelera;

/**
 * @param terminoEn {@code null} mientras la corrida no volvió
 * @param detalle el log de la corrida, o el motivo si falló; puede ser {@code null}
 */
public record ImportacionVistaDTO(int id, String estado, int paginas, String pedidaEn,
                                  String terminoEn, int nuevas, int salteadas, int fallidas,
                                  String detalle) {
}
