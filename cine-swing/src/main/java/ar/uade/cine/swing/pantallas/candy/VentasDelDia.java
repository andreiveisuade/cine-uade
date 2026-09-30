package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Formato;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.TablaCompras;
import ar.uade.cine.swing.pantallas.Seccion;
import com.toedter.calendar.JDateChooser;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// La pestaña con las ventas de candy de un día; el total cobrado está en Caja, al lado de la boletería.
final class VentasDelDia extends Seccion {

    private final ApiCandy apiCandy;
    private final JDateChooser fecha = Fechas.selector(LocalDate.now());
    private final JLabel cantidad = new JLabel(" ");
    private final Tabla<CompraCandy> tabla = TablaCompras.crear();

    VentasDelDia(ApiCandy apiCandy) {
        super(new BorderLayout(0, 8));
        this.apiCandy = apiCandy;
        cantidad.setFont(cantidad.getFont().deriveFont(Font.BOLD, 18f));
        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Fecha"));
        barra.add(fecha);
        barra.add(cantidad);
        add(barra, BorderLayout.NORTH);
        add(tabla.conScroll(), BorderLayout.CENTER);
        add(Componentes.nota("El total cobrado del día está en Caja, al lado de la boletería."), BorderLayout.SOUTH);
        fecha.addPropertyChangeListener("date", e -> recargar());
        recargar();
    }

    private void recargar() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("fecha", Fechas.iso(fecha));
        cargar(() -> apiCandy.obtenerComprasCandy(filtros), compras -> {
            cantidad.setText(Formato.cantidad(compras.size(), "venta", "ventas"));
            tabla.mostrar(compras);
        });
    }
}
