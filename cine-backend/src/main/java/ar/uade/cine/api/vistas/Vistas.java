package ar.uade.cine.api.vistas;

import ar.uade.cine.Aplicacion;

/**
 * Los ensambladores de la API, ya armados y conectados entre sí.
 *
 * <p>Existe por la misma razón que {@link Aplicacion}: hay un solo orden posible para
 * armarlos —{@link VistasVentas} necesita a {@link VistasCartelera}, que necesita a
 * {@link VistasSalas}— y ese orden tiene que estar escrito una sola vez. Cuando el armado
 * vivía adentro de {@code ServidorApi}, esa clase hacía tres cosas: configurar el
 * servidor, armar los ensambladores y cablear las rutas.
 *
 * <p>Es un record y no una clase con getters porque no es más que la bolsa de los seis:
 * no decide nada, y cada componente se entrega tal cual a quien lo pide.
 *
 * <p>Que las vistas se entreguen por separado y no como un objeto único es a propósito, y
 * es lo que hace que cada familia de rutas reciba <strong>solo</strong> la que usa. Pasarle
 * este record entero a cada {@code Rutas*} sería más corto de escribir y le daría a la ruta
 * de sesión acceso a la vista de ventas, que es exactamente lo que se quitó del medio
 * cuando existía una sola clase Vistas para todo.
 */
public record Vistas(VistasSalas salas, VistasCartelera cartelera, VistasUsuarios usuarios,
                     VistasPromociones promociones, VistasCandy candy, VistasVentas ventas) {

    /** El único armado posible, en el único orden que las dependencias permiten. */
    public static Vistas de(Aplicacion aplicacion) {
        VistasSalas salas = new VistasSalas(aplicacion.getSalas(),
                aplicacion.getCalculadoraPrecio());
        VistasCartelera cartelera = new VistasCartelera(aplicacion.getCartelera(),
                aplicacion.getSalas(), aplicacion.getOcupacion(),
                aplicacion.getCalculadoraPrecio(), salas);
        VistasUsuarios usuarios = new VistasUsuarios();
        VistasPromociones promociones = new VistasPromociones();
        VistasCandy candy = new VistasCandy();
        VistasVentas ventas = new VistasVentas(aplicacion.getFunciones(), aplicacion.getSalas(),
                aplicacion.getCartelera(), aplicacion.getClientes(), aplicacion.getPagos(),
                aplicacion.getReservas(), cartelera, salas, usuarios);

        return new Vistas(salas, cartelera, usuarios, promociones, candy, ventas);
    }
}
