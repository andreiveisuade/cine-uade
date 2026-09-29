package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiInformes;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.informes.Arqueo;
import ar.uade.cine.swing.api.dto.informes.ArqueoCandy;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.TablaCompras;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/** El arqueo del día. Boletería y candy se cuentan por separado: el candy de mostrador no tiene función ni reserva. */
final class PantallaCaja extends Pantalla {

    private record Datos(Arqueo arqueo, ArqueoCandy candy) {
    }

    private final ApiInformes apiInformes;
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
    private final Tabla<CompraCandy> candy = TablaCompras.crear();

    PantallaCaja(ApiInformes apiInformes) {
        super("Arqueo", "Lo cobrado en el día, por medio de pago: boletería y candy, cada una con su caja.");
        this.apiInformes = apiInformes;

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

    private void mover(int dias) {
        LocalDate actual = Fechas.leer(fecha);
        // Cambiar la fecha dispara la recarga por el listener de "date".
        Fechas.poner(fecha, (actual == null ? LocalDate.now() : actual).plusDays(dias));
    }

    private void recargar() {
        String dia = Fechas.iso(fecha);
        cargar(() -> new Datos(apiInformes.obtenerArqueo(dia), apiInformes.obtenerArqueoCandy(dia)), this::pintar);
    }

    private void pintar(Datos datos) {
        Arqueo arqueo = datos.arqueo();
        cifras.removeAll();
        cifras.add(Componentes.cifra("Boletería", precio(arqueo.total()), null, null));
        cifras.add(Componentes.cifra("Candy", precio(datos.candy().total()), null, null));
        cifras.add(Componentes.cifra("Operaciones", String.valueOf(arqueo.pagos().size()), null, null));
        cifras.add(Componentes.cifra("Entradas", String.valueOf(arqueo.entradas()), null, null));
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
}
