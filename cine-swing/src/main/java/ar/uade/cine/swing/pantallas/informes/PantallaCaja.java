package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.ApiInformes;
import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.informes.Arqueo;
import ar.uade.cine.swing.api.dto.informes.ArqueoCandy;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.TablaCompras;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// El arqueo del día, boletería y candy por separado: el candy de mostrador no tiene función ni reserva.
public final class PantallaCaja extends Pantalla {

    private record Datos(Arqueo arqueo, ArqueoCandy candy) {
    }

    private static final int MAX_FILAS = 8;

    private final ApiInformes apiInformes;
    private final CampoFecha fecha = new CampoFecha(LocalDate.now());
    private final JPanel cifras = new JPanel(new GridLayout(1, 4, Escala.CERCA, 0));
    private final JLabel tituloCandy = Componentes.subtitulo("Candy");
    private final Tabla<Pago> boleteria = new Tabla<>(
            Columna.<Pago>de("Hora", p -> hora(p.fecha())).ancho(60),
            Columna.<Pago>de("Reserva", p -> "#" + p.reservaId()).ancho(70),
            Columna.<Pago>de("Película", p -> p.pelicula() == null ? "—" : p.pelicula().titulo()).ancho(220),
            Columna.<Pago>de("Cliente", p -> p.cliente() == null ? "—" : p.cliente().nombre()),
            Columna.<Pago>de("Medio", p -> etiqueta(p.medio())),
            Columna.<Pago>de("Autorización", p -> p.codigoAutorizacion() == null ? "—" : p.codigoAutorizacion()),
            Columna.<Pago>numero("Descuento", p -> p.descuento() > 0 ? "−" + precio(p.descuento()) : "—"),
            Columna.<Pago>numero("Monto", p -> precio(p.monto())))
            .vacia("No se cobró nada en boletería ese día");
    private final Tabla<CompraCandy> candy = TablaCompras.crear().vacia("No se vendió candy ese día");

    public PantallaCaja(ApiInformes apiInformes) {
        super("Caja", "El arqueo del día: lo cobrado en boletería y en candy, por medio de pago.");
        this.apiInformes = apiInformes;

        JPanel barra = new JPanel(new FlujoConSalto());
        JButton anterior = new JButton("◀");
        anterior.setToolTipText("Día anterior");
        JButton siguiente = new JButton("▶");
        siguiente.setToolTipText("Día siguiente");
        barra.add(new JLabel("Fecha"));
        barra.add(anterior);
        barra.add(fecha);
        barra.add(siguiente);
        anterior.addActionListener(e -> mover(-1));
        siguiente.addActionListener(e -> mover(1));
        fecha.alCambiar(this::recargar);

        // Las tablas miden lo que tienen: un día flojo no deja medio pantalla vacía entre boletería y candy.
        Pila contenido = new Pila();
        contenido.agregar(barra).espacio(Escala.CERCA)
                .agregar(cifras).espacio(Escala.BLOQUE)
                .agregar(Componentes.subtitulo("Boletería")).espacio(Escala.JUNTO)
                .agregar(boleteria.conScrollAjustado(MAX_FILAS)).espacio(Escala.BLOQUE)
                .agregar(tituloCandy).espacio(Escala.JUNTO)
                .agregar(candy.conScrollAjustado(MAX_FILAS));
        add(Componentes.arriba(contenido), BorderLayout.CENTER);
        recargar();
    }

    private void mover(int dias) {
        LocalDate actual = fecha.valor();
        // Cambiar la fecha dispara la recarga por el listener de "date".
        fecha.poner((actual == null ? LocalDate.now() : actual).plusDays(dias));
    }

    private void recargar() {
        String dia = fecha.iso();
        cargar(() -> new Datos(apiInformes.obtenerArqueo(dia), apiInformes.obtenerArqueoCandy(dia)), this::pintar);
    }

    private void pintar(Datos datos) {
        Arqueo arqueo = datos.arqueo();
        // El desglose por medio va dentro de la cifra: es lo que se cuenta contra la caja física.
        String porMedio = arqueo.porMedio().isEmpty() ? "Sin cobros"
                : arqueo.porMedio().entrySet().stream()
                .map(e -> etiqueta(e.getKey()) + " · " + precio(e.getValue().total()))
                .collect(Collectors.joining("<br>"));
        Componentes.reemplazar(cifras,
                Componentes.cifra("Boletería", precio(arqueo.total()), porMedio, null),
                Componentes.cifra("Candy", precio(datos.candy().total()), null, null),
                Componentes.cifra("Cobros", String.valueOf(arqueo.pagos().size()), null, null),
                Componentes.cifra("Entradas", String.valueOf(arqueo.entradas()), null, null));
        boleteria.mostrar(arqueo.pagos());
        tituloCandy.setText("Candy · " + cantidad(datos.candy().compras().size(), "venta", "ventas"));
        candy.mostrar(datos.candy().compras());
    }
}
