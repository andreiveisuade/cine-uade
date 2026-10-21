package ar.uade.cine.model.promociones;

// Lo propio de cada tipo de promoción: porcentaje, monto, lleva y paga; record plano de ida y vuelta.
// Sin validación propia: qué hace falta lo dice cada subclase al construirse.
public record ParametrosPromocion(Double porcentaje, Double monto, Integer lleva, Integer paga) {


    public static ParametrosPromocion dePorcentaje(Double porcentaje) {
        return new ParametrosPromocion(porcentaje, null, null, null);
    }

    public static ParametrosPromocion deMonto(Double monto) {
        return new ParametrosPromocion(null, monto, null, null);
    }

    public static ParametrosPromocion deNxM(Integer lleva, Integer paga) {
        return new ParametrosPromocion(null, null, lleva, paga);
    }
}
