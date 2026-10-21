package ar.uade.cine.model.promociones;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;

// Los tres tipos de promoción y cómo nace cada uno; Factory Method: cada constante crea su subclase.
// Patrón Factory Method: sumar un tipo es una constante y una subclase, sin tocar controller ni vistas.
@Getter
public enum TipoPromocion {

    PORCENTAJE("porcentaje") {
        @Override
        public Promocion crear(String nombre, ParametrosPromocion parametros, CondicionesPromocion condiciones,
                               LocalDate hoy) {
            return new PromocionPorcentaje(nombre, parametros.porcentaje(), condiciones, hoy);
        }
    },

    MONTO_FIJO("monto") {
        @Override
        public Promocion crear(String nombre, ParametrosPromocion parametros, CondicionesPromocion condiciones,
                               LocalDate hoy) {
            return new PromocionMontoFijo(nombre, parametros.monto(), condiciones, hoy);
        }
    },

    NXM("lleva", "paga") {
        @Override
        public Promocion crear(String nombre, ParametrosPromocion parametros, CondicionesPromocion condiciones,
                               LocalDate hoy) {
            return new PromocionNxM(nombre, parametros.lleva(), parametros.paga(), condiciones, hoy);
        }
    };

    // Los campos del JSON que pide el alta de cada tipo; los publica GET /api/tipos-promocion.
    private final List<String> campos;

    TipoPromocion(String... campos) {
        this.campos = List.of(campos);
    }

    // hoy lo pasa el gestor desde el reloj: una promoción que ya no va a aplicar nunca no se crea.
    public abstract Promocion crear(String nombre, ParametrosPromocion parametros,
                                    CondicionesPromocion condiciones, LocalDate hoy);
}
