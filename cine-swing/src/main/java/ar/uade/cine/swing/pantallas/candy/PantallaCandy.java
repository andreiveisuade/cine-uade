package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.stream.Collectors;

// La otra caja del cine (CU-13 a CU-16): carta, venta de mostrador y ventas del día, una pestaña cada una.
/** Pestañas y no una pantalla larga: cobrar un pochoclo no puede pedir scrollear entre combos. */
public final class PantallaCandy extends Pantalla {

    public PantallaCandy(ApiCandy apiCandy, ApiCatalogos apiCatalogos, ApiClientes apiClientes) {
        super("Candy", "La otra caja del cine: se cobra en el mostrador y se entrega, sin reserva de por medio.");
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Carta", new Carta(apiCandy, apiCatalogos));
        pestanas.addTab("Venta de mostrador", new VentaMostrador(apiCandy, apiCatalogos, apiClientes));
        pestanas.addTab("Ventas del día", new VentasDelDia(apiCandy));
        add(pestanas, BorderLayout.CENTER);
    }

    /** Lo que trae un combo, "2× Pochoclos + 1× Gaseosa"; un producto suelto no trae nada. */
    static String componentesDe(Producto p) {
        if (p.componentes() == null || p.componentes().isEmpty()) return "—";
        return p.componentes().stream().map(c -> c.cantidad() + "× " + c.nombre()).collect(Collectors.joining(" + "));
    }
}
