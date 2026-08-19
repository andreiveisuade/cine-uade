package ar.uade.cine.dto.cartelera;

/**
 * Una corrida del importador para el front: las fechas ya en texto ISO y el estado con el
 * nombre de la constante, como todos los enums del contrato.
 *
 * @param terminoEn {@code null} mientras la corrida no volvió
 * @param detalle el log de la corrida, o el motivo si falló. Puede ser {@code null}
 */
public record ImportacionVistaDTO(int id, String estado, int paginas, String pedidaEn,
                                  String terminoEn, int nuevas, int salteadas, int fallidas,
                                  String detalle) {
}
