package ar.uade.cine.ui;

import java.util.List;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.persistencia.ButacaOcupadaException;

/**
 * El menú principal: lista las áreas del cine y delega en la que se elija.
 *
 * <p>No sabe qué hace ninguna. Recibe una lista de {@link Menu}, la numera y llama al que
 * corresponde, así que sumar un área es escribir su menú y agregarlo a la lista —no tocar
 * un switch que ya tenía ocho ramas—.
 *
 * <p>Lo que sí es suyo es el manejo de errores: los gestores rechazan con
 * IllegalArgumentException, y acá esa excepción se convierte en un mensaje y en volver al
 * menú, en vez de cortar el programa. Es el mismo papel que cumple la capa HTTP cuando la
 * traduce a un 400.
 */
public class MenuConsola {

    private final Consola consola;
    private final List<Menu> menus;

    public MenuConsola(Consola consola, List<Menu> menus) {
        this.consola = consola;
        this.menus = List.copyOf(menus);
    }

    /** Los menús del cine, en el orden en que se muestran. */
    public static MenuConsola delCine(Aplicacion aplicacion) {
        Consola consola = new Consola();
        MapaDeButacas mapa = new MapaDeButacas(consola, aplicacion.getSalas(),
                aplicacion.getFunciones(), aplicacion.getReservas());

        return new MenuConsola(consola, List.of(
                new MenuPeliculas(consola, aplicacion.getCartelera()),
                new MenuSalas(consola, aplicacion.getSalas(), mapa),
                new MenuFunciones(consola, aplicacion.getFunciones(), aplicacion.getCartelera(),
                        aplicacion.getSalas()),
                new MenuClientes(consola, aplicacion.getClientes()),
                new MenuReservas(consola, aplicacion.getReservas(), aplicacion.getPagos(),
                        aplicacion.getFunciones(), aplicacion.getClientes(), mapa),
                new MenuCandy(consola, aplicacion.getCandy(), aplicacion.getClientes()),
                new MenuArqueo(consola, aplicacion.getPagos(), aplicacion.getCandy()),
                new MenuEmpleados(consola, aplicacion.getEmpleados(), aplicacion.getReservas(),
                        aplicacion.getFunciones(), aplicacion.getCartelera(), aplicacion.getSalas())));
    }

    public void iniciar() {
        while (true) {
            mostrarOpciones();
            String opcion = leerOpcion();
            if ("0".equals(opcion)) {
                return;
            }
            atender(opcion);
        }
    }

    private void mostrarOpciones() {
        consola.mostrar("\n===== CINE =====");
        for (int i = 0; i < menus.size(); i++) {
            consola.mostrar((i + 1) + ". " + menus.get(i).titulo());
        }
        consola.mostrar("0. Salir");
        consola.pedir("Opción: ");
    }

    private String leerOpcion() {
        return consola.leer();
    }

    private void atender(String opcion) {
        try {
            elegido(opcion).mostrar();
        } catch (NumberFormatException e) {
            consola.mostrar("Se esperaba un número");
        } catch (IllegalArgumentException e) {
            consola.mostrar(e.getMessage());
        } catch (ButacaOcupadaException e) {
            // Se la ganaron por milisegundos: no es un error de quien opera.
            consola.mostrar(e.getMessage() + ". Volvé a elegir sobre el mapa actualizado.");
        }
    }

    /** Opción inválida no es una excepción: se avisa y se vuelve a mostrar el menú. */
    private Menu elegido(String opcion) {
        try {
            int numero = Integer.parseInt(opcion);
            if (numero >= 1 && numero <= menus.size()) {
                return menus.get(numero - 1);
            }
        } catch (NumberFormatException e) {
            // cae en el mismo lugar que un número fuera de rango
        }
        return new MenuInvalido(consola);
    }

    /** Lo que se muestra cuando la opción no existe. */
    private record MenuInvalido(Consola consola) implements Menu {

        @Override
        public String titulo() {
            return "";
        }

        @Override
        public void mostrar() {
            consola.mostrar("Opción inválida");
        }
    }
}
