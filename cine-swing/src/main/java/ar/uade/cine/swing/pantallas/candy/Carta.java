package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.dto.candy.PedidoCombo;
import ar.uade.cine.swing.api.dto.candy.PedidoProducto;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

// La pestaña de la carta: altas de productos y combos, edición, y sacar o reponer; nada se borra.
final class Carta extends Seccion {

    private final ApiCandy apiCandy;
    private final JButton editar = new JButton("Editar");
    private final JButton alternar = new JButton("Sacar de la carta");
    private final Tabla<Producto> tabla = new Tabla<>(
            Columna.<Producto>de("Producto", Producto::nombre).ancho(200),
            Columna.<Producto>de("Tipo", p -> etiqueta(p.tipo())),
            Columna.<Producto>de("Trae", PantallaCandy::componentesDe).ancho(220),
            Columna.<Producto>numero("Precio", p -> precio(p.precio())),
            Columna.<Producto>de("Estado", p -> p.disponible() ? "A la venta" : "Fuera de la carta"));

    private final JTextField nombreProducto = new JTextField();
    private final JComboBox<Opcion<String>> tipoProducto = new JComboBox<>();
    private final JTextField precioProducto = Campos.soloDecimal(new JTextField());
    private final JLabel errorProducto = Componentes.texto(" ");

    private final JTextField nombreCombo = new JTextField();
    private final JTextField precioCombo = Campos.soloDecimal(new JTextField());
    private final JPanel filasCombo = new JPanel(new GridBagLayout());
    private final Cantidades cantidadesCombo = new Cantidades();
    private final JLabel errorCombo = Componentes.texto(" ");

    Carta(ApiCandy apiCandy, ApiCatalogos apiCatalogos) {
        super(new BorderLayout(12, 8));
        this.apiCandy = apiCandy;
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(editar);
        acciones.add(alternar);
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(acciones, BorderLayout.NORTH);
        abajo.add(Componentes.nota("Los productos no se borran: se sacan de la carta. Uno que ya se vendió tiene "
                + "que seguir existiendo para que el ticket de esa venta diga qué se llevó. Cambiar un precio no "
                + "toca las ventas hechas: cada una guardó el precio que tenía."), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(abajo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(altas(), BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        tabla.alDobleClic(this::editar);
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(this::editar));
        alternar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (p.disponible() && !confirmar("¿Sacar " + p.nombre() + " de la carta? Deja de venderse en el "
                    + "mostrador.", "Sí, sacar")) return;
            accion(() -> apiCandy.cambiarDisponibilidadCandy(p.id(), !p.disponible()),
                    p.nombre() + (p.disponible() ? " salió de la carta" : " volvió a la carta"), this::recargar);
        }));
        habilitar();
        // Qué tipos se dan de alta sueltos lo dice el catálogo: el combo se arma abajo, con sus componentes.
        cargar(apiCatalogos::obtenerTiposProducto, tipos -> tipos.stream().filter(t -> !t.esCombo())
                .forEach(t -> tipoProducto.addItem(new Opcion<>(t.nombre(), etiqueta(t.nombre())))));
        recargar();
    }

    private void habilitar() {
        var elegido = tabla.seleccionada();
        editar.setEnabled(elegido.isPresent());
        alternar.setEnabled(elegido.isPresent());
        alternar.setText(elegido.map(p -> p.disponible() ? "Sacar de la carta" : "Reponer")
                .orElse("Sacar de la carta"));
    }

    private JScrollPane altas() {
        JButton agregar = new JButton("Agregar a la carta");
        agregar.addActionListener(e -> crearProducto());
        JButton armar = new JButton("Armar combo");
        armar.addActionListener(e -> armarCombo());

        // El de combo abajo y estirado: así el costado llega al fondo como en las demás pantallas.
        JPanel columna = new JPanel(new BorderLayout(0, 8));
        columna.add(Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Nuevo producto"))
                .obligatorio("Nombre", nombreProducto)
                .obligatorio("Tipo", tipoProducto)
                .obligatorio("Precio", precioProducto)
                .ancho(agregar)
                .ancho(errorProducto)), BorderLayout.NORTH);
        columna.add(Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Armar combo"))
                .ancho(Componentes.nota("Al menos dos productos. El combo tiene que salir menos que sus "
                        + "componentes sueltos (R14): si no, no habría motivo para ofrecerlo."))
                .obligatorio("Nombre", nombreCombo)
                .ancho(new JLabel("Qué trae (cantidad)"))
                .ancho(filasCombo)
                .obligatorio("Precio del combo", precioCombo)
                .ancho(armar)
                .ancho(errorCombo)
                .cerrar()), BorderLayout.CENTER);
        return Componentes.lateral(columna);
    }

    private void recargar() {
        cargar(() -> apiCandy.obtenerProductosCandy(true), productos -> {
            tabla.mostrar(productos);
            pintarComponentes(productos.stream().filter(p -> !p.esCombo()).toList());
            habilitar();
        });
    }

    private void pintarComponentes(List<Producto> sueltos) {
        filasCombo.removeAll();
        cantidadesCombo.olvidar();
        if (sueltos.isEmpty()) {
            filasCombo.add(Componentes.nota("Primero cargá productos sueltos."));
        }
        int fila = 0;
        for (Producto p : sueltos) {
            JLabel nombre = new JLabel(p.nombre() + "  · " + precio(p.precio()));
            if (!p.disponible()) nombre.setForeground(Componentes.gris());
            JSpinner spinner = cantidadesCombo.nueva(p.id());
            GridBagConstraints izquierda = new GridBagConstraints();
            izquierda.gridy = fila;
            izquierda.weightx = 1;
            izquierda.anchor = GridBagConstraints.WEST;
            izquierda.insets = new Insets(2, 0, 2, 8);
            filasCombo.add(nombre, izquierda);
            GridBagConstraints derecha = new GridBagConstraints();
            derecha.gridy = fila++;
            derecha.gridx = 1;
            filasCombo.add(spinner, derecha);
        }
        filasCombo.revalidate();
        filasCombo.repaint();
    }

    private void crearProducto() {
        Validacion v = new Validacion(errorProducto);
        String nombre = v.texto(nombreProducto, "Nombre", true);
        String tipo = v.elegido(tipoProducto, "Tipo");
        Double precio = v.decimal(precioProducto, "Precio", true);
        if (!v.ok()) return;
        PedidoProducto pedido = new PedidoProducto(nombre, tipo, precio);
        Tarea.ejecutar(this, () -> apiCandy.crearProductoCandy(pedido), creado -> {
            avisar("Producto agregado");
            nombreProducto.setText("");
            precioProducto.setText("");
            recargar();
        }, v::mostrarError);
    }

    // El mínimo de dos productos y R14 los valida el backend: su mensaje aparece abajo del formulario.
    private void armarCombo() {
        Validacion v = new Validacion(errorCombo);
        String nombre = v.texto(nombreCombo, "Nombre", true);
        Double precio = v.decimal(precioCombo, "Precio del combo", true);
        v.alMencionar("combo", precioCombo);
        if (!v.ok()) return;
        PedidoCombo pedido = new PedidoCombo(nombre, precio, cantidadesCombo.elegidas());
        Tarea.ejecutar(this, () -> apiCandy.armarComboCandy(pedido), creado -> {
            avisar("Combo armado");
            nombreCombo.setText("");
            precioCombo.setText("");
            recargar();
        }, v::mostrarError);
    }

    private void editar(Producto p) {
        new EdicionProducto(this, apiCandy, p, this::recargar).abrir();
    }
}
