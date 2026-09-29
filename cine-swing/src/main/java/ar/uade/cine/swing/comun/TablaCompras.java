package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.comun.Tabla.Columna;

import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Las ventas de candy como las listan Caja y Candy: las dos pantallas muestran las mismas columnas.
public final class TablaCompras {

    private TablaCompras() {
    }

    public static Tabla<CompraCandy> crear() {
        return new Tabla<>(
                Columna.<CompraCandy>de("Hora", c -> hora(c.fecha())).ancho(60),
                Columna.<CompraCandy>de("Venta", c -> "#" + c.id()).ancho(60),
                Columna.<CompraCandy>de("Qué se llevó", c -> c.items().stream()
                        .map(i -> i.cantidad() + "× " + i.nombre()).collect(Collectors.joining(", "))).ancho(260),
                Columna.<CompraCandy>de("Reserva", c -> c.reservaId() == null ? "—" : "#" + c.reservaId()),
                Columna.<CompraCandy>de("Medio", c -> etiqueta(c.medio())),
                Columna.<CompraCandy>de("Autorización",
                        c -> c.codigoAutorizacion() == null || c.codigoAutorizacion().isEmpty()
                                ? "—" : c.codigoAutorizacion()),
                Columna.<CompraCandy>numero("Ahorro", c -> c.ahorro() > 0 ? precio(c.ahorro()) : "—"),
                Columna.<CompraCandy>numero("Total", c -> precio(c.total())));
    }
}
