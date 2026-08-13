package ar.uade.cine;

import ar.uade.cine.ui.MenuConsola;

/**
 * La puerta de consola. Lo único que hace es levantar la aplicación y abrir el menú:
 * qué se guarda dónde lo decide {@link Aplicacion}, que es la misma que usa la API.
 */
public class Main {

    public static void main(String[] args) {
        Aplicacion aplicacion = Aplicacion.enMySQL();

        new MenuConsola(aplicacion.getCartelera(), aplicacion.getSalas(), aplicacion.getFunciones(),
                aplicacion.getClientes(), aplicacion.getReservas(), aplicacion.getEmpleados(),
                aplicacion.getPagos(), aplicacion.getCandy()).iniciar();
    }
}
