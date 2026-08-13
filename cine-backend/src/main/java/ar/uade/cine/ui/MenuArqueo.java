package ar.uade.cine.ui;

import java.time.LocalDate;

import ar.uade.cine.servicio.Arqueo;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorPagos;

/**
 * El cierre de caja del día.
 *
 * <p>Las dos cajas del cine son distintas: la boletería cobra reservas y el candy vende en
 * el mostrador. Se muestran separadas y después se suman, que es como se cuenta la plata
 * al cerrar.
 */
class MenuArqueo implements Menu {

    private final Consola consola;
    private final GestorPagos pagos;
    private final GestorCandy candy;

    MenuArqueo(Consola consola, GestorPagos pagos, GestorCandy candy) {
        this.consola = consola;
        this.pagos = pagos;
        this.candy = candy;
    }

    @Override
    public String titulo() {
        return "Arqueo del día";
    }

    @Override
    public void mostrar() {
        LocalDate dia = LocalDate.now();
        Arqueo arqueo = pagos.arqueoDe(dia);

        consola.mostrar("\n-- Boletería --");
        consola.imprimir(arqueo.pagos());
        consola.mostrar("\n-- Candy --");
        consola.imprimir(candy.listarComprasDelDia(dia));

        double boleteria = arqueo.total();
        double barra = candy.totalVendido(dia);
        consola.mostrar(String.format("%nBoletería : $ %10.2f", boleteria));
        consola.mostrar(String.format("Candy     : $ %10.2f", barra));
        consola.mostrar(String.format("Total del %s: $ %10.2f", dia, boleteria + barra));
    }
}
