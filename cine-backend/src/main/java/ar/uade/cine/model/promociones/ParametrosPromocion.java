package ar.uade.cine.model.promociones;

// Lo propio de cada tipo de promoción: porcentaje, monto, lleva y paga; record plano de ida y vuelta.
// De ida es lo que trae el pedido: TipoPromocion.crear le pasa a cada subclase solo lo suyo, y lo ajeno
// al tipo se ignora. De vuelta, cada subclase describe los suyos en getParametros() y deja el resto en
// null, así la vista los manda sin preguntar de qué clase es. Sin validación propia: qué hace falta
// depende del tipo, y eso lo dice cada subclase al construirse.
public record ParametrosPromocion(Double porcentaje, Double monto, Integer lleva, Integer paga) {

    // Uno por tipo, con null en lo ajeno: new ParametrosPromocion(30.0, null, null, null) no dice de quién es.

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
