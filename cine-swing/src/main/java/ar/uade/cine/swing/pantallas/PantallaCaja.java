package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Arqueo;
import ar.uade.cine.swing.api.dto.ArqueoCandy;
import ar.uade.cine.swing.api.dto.CompraCandy;
import ar.uade.cine.swing.api.dto.Pago;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/** El arqueo del día. Boletería y candy se cuentan por separado: el candy de mostrador no tiene función ni reserva. */
final class PantallaCaja extends Pantalla {

    private record Datos(Arqueo arqueo, ArqueoCandy candy) {
    }

    private final JDateChooser fecha = Fechas.selector(LocalDate.now());
    private final JPanel cifras = new JPanel(new FlujoConSalto());
    private final JLabel porMedio = new JLabel(" ");
    private final JLabel tituloCandy = Componentes.subtitulo("Candy");
    private final Tabla<Pago> boleteria = new Tabla<>(
            Columna.<Pago>de("Hora", p -> hora(p.fecha())).ancho(60),
            Columna.<Pago>de("Reserva", p -> "#" + p.reservaId()).ancho(70),
            Columna.<Pago>de("Película", p -> p.pelicula() == null ? "—" : p.pelicula().titulo()).ancho(220),
            Columna.<Pago>de("Cliente", p -> p.cliente() == null ? "—" : p.cliente().nombre()),
            Columna.<Pago>de("Medio", p -> etiqueta(p.medio())),
            Columna.<Pago>de("Autorización", p -> p.codigoAutorizacion() == null ? "—" : p.codigoAutorizacion()),
            Columna.<Pago>numero("Descuento", p -> p.descuento() > 0 ? "−" + precio(p.descuento()) : "—"),
            Columna.<Pago>numero("Monto", p -> precio(p.monto())));
    private final Tabla<CompraCandy> candy = tablaCompras();

    PantallaCaja(ApiHttp api) {
        super(api, "Arqueo", "Lo cobrado en el día, por medio de pago: boletería y candy, cada una con su caja.");

        JPanel barra = new JPanel(new FlujoConSalto());
        JButton anterior = new JButton("◀");
        JButton siguiente = new JButton("▶");
        barra.add(new JLabel("Fecha"));
        barra.add(anterior);
        barra.add(fecha);
        barra.add(siguiente);
        anterior.addActionListener(e -> mover(-1));
        siguiente.addActionListener(e -> mover(1));
        fecha.addPropertyChangeListener("date", e -> recargar());

        JPanel resumen = new JPanel();
        resumen.setLayout(new BoxLayout(resumen, BoxLayout.Y_AXIS));
        resumen.add(Componentes.izquierda(barra));
        resumen.add(Componentes.izquierda(cifras));
        resumen.add(Componentes.izquierda(porMedio));
        porMedio.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 0));

        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(Componentes.subtitulo("Boletería"), BorderLayout.NORTH);
        arriba.add(boleteria.conScroll());
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(tituloCandy, BorderLayout.NORTH);
        abajo.add(candy.conScroll());
        JSplitPane tablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        tablas.setResizeWeight(0.6);
        tablas.setBorder(null);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(resumen, BorderLayout.NORTH);
        centro.add(tablas, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        recargar();
    }

    static Tabla<CompraCandy> tablaCompras() {
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

    private void mover(int dias) {
        LocalDate actual = Fechas.leer(fecha);
        // Cambiar la fecha dispara la recarga por el listener de "date".
        Fechas.poner(fecha, (actual == null ? LocalDate.now() : actual).plusDays(dias));
    }

    private void recargar() {
        String dia = Fechas.iso(fecha);
        cargar(() -> new Datos(api.obtenerArqueo(dia), api.obtenerArqueoCandy(dia)), this::pintar);
    }

    private void pintar(Datos datos) {
        Arqueo arqueo = datos.arqueo();
        cifras.removeAll();
        cifras.add(cifra("Boletería", precio(arqueo.total())));
        cifras.add(cifra("Candy", precio(datos.candy().total())));
        cifras.add(cifra("Operaciones", String.valueOf(arqueo.pagos().size())));
        cifras.add(cifra("Entradas", String.valueOf(arqueo.entradas())));
        cifras.revalidate();
        cifras.repaint();
        porMedio.setText(arqueo.porMedio().isEmpty() ? "No se cobró nada en boletería ese día."
                : arqueo.porMedio().entrySet().stream()
                .map(e -> etiqueta(e.getKey()) + " · " + e.getValue().cantidad() + "  " + precio(e.getValue().total()))
                .collect(Collectors.joining("     ")));
        boleteria.mostrar(arqueo.pagos());
        tituloCandy.setText("Candy · " + datos.candy().compras().size() + " ventas");
        candy.mostrar(datos.candy().compras());
    }

    private static JPanel cifra(String titulo, String valor) {
        JLabel arriba = new JLabel(titulo.toUpperCase());
        arriba.setForeground(Componentes.gris());
        arriba.setFont(arriba.getFont().deriveFont(11f));
        JLabel numero = new JLabel(valor);
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, 22f));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(numero, BorderLayout.CENTER);
        return Componentes.conBorde(panel);
    }
}
