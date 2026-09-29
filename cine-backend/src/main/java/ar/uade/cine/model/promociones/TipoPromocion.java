package ar.uade.cine.model.promociones;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;

// Los tres tipos de promoción y cómo nace cada uno; Factory Method: cada constante crea su subclase.
//
// Qué patrón es: Factory Method sobre un enum con cuerpo por constante. crear() es abstracto, y cada
// constante lo implementa devolviendo su subclase de Promocion. Quien da de alta una promoción nombra
// el tipo y nunca la clase concreta.
//
// Qué problema resolvía: el tipo se preguntaba en tres lugares a la vez. PromocionController tenía un
// switch que elegía uno de tres crear* del gestor, VistasPromociones una cadena de instanceof para
// saber qué campos mandar, y este enum un exigirCampos(Object...) posicional, que dependía de que el
// gestor pasara los valores en el mismo orden que la lista de campos. Sumar un tipo era tocar los tres
// lugares y acordarse de todos; olvidarse de uno compilaba igual.
//
// Cómo se lee: TipoPromocion.NXM.crear(nombre, parametros, condiciones, hoy) hace new PromocionNxM con
// lleva y paga, y es PromocionNxM la que dice qué le falta. Es la mitad del polimorfismo; la otra mitad
// está en Promocion: getParametros() y calcularDescuento() los contesta cada subclase. Un tipo nuevo es
// una constante más acá y una subclase: el compilador no deja olvidarse de crear() ni de getParametros().
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

    // Los campos del JSON que pide el alta de cada tipo: los publica GET /api/tipos-promocion, así el
    // formulario muestra solo los que van. Qué hacer con ellos lo sabe crear().
    private final List<String> campos;

    TipoPromocion(String... campos) {
        this.campos = List.of(campos);
    }

    // hoy lo pasa el gestor desde el reloj: una promoción que ya no va a aplicar nunca no se crea.
    public abstract Promocion crear(String nombre, ParametrosPromocion parametros,
                                    CondicionesPromocion condiciones, LocalDate hoy);
}
