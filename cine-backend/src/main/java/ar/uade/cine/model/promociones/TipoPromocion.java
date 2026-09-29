package ar.uade.cine.model.promociones;

import java.util.List;
import java.util.Map;

import lombok.Getter;

// Los tres tipos de promoción; Experto en los campos propios que exige cada uno al crearla.
// Además de nombre y condiciones, cada tipo pide sus propios campos del pedido. Es la única
// lista: la publica el catálogo y con ella GestorPromociones dice cuál falta.
@Getter
public enum TipoPromocion {

    PORCENTAJE("porcentaje"),

    MONTO_FIJO("monto"),

    NXM("lleva", "paga");

    // El catálogo publica el nombre del campo en el JSON; el mensaje dice qué es, con la forma "Falta el X".
    // El del monto es el mismo texto que da PromocionMontoFijo si le llega sin monto.
    private static final Map<String, String> QUE_ES = Map.of(
            "porcentaje", "el porcentaje",
            "monto", "el monto del descuento",
            "lleva", "cuántas entradas lleva",
            "paga", "cuántas entradas paga");

    private final List<String> campos;

    TipoPromocion(String... campos) {
        this.campos = List.of(campos);
    }

    // Los valores en el orden de getCampos(): el primero que falte se nombra en el mensaje.
    public void exigirCampos(Object... valores) {
        for (int i = 0; i < campos.size(); i++) {
            if (valores[i] == null) {
                throw new IllegalArgumentException("Falta " + QUE_ES.get(campos.get(i)));
            }
        }
    }
}
