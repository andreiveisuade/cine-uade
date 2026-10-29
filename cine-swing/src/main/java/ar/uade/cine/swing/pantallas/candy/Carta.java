package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.dto.candy.PedidoProducto;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.comun.CampoImagen;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;
import ar.uade.cine.swing.pantallas.acciones.BotonesDeFila;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.precio;

// La pestaña de la carta: alta de productos (los combos, en FormularioCombo), edición, y sacar o reponer; nada se borra.
final class Carta extends Seccion {

    private final ApiCandy apiCandy;
    private final ApiImagenes apiImagenes;
    private final JButton editar = new JButton("Editar");
    private final Tabla<Producto> tabla = new Tabla<>(
            Columna.<Producto>de("Producto", Producto::nombre).ancho(200),
            Columna.<Producto>de("Tipo", p -> etiqueta(p.tipo())).ancho(100),
            Columna.<Producto>de("Trae", PantallaCandy::componentesDe).ancho(180),
            Columna.<Producto>numero("Precio", p -> precio(p.precio())),
            Columna.<Producto>de("Estado", p -> p.disponible() ? "A la venta" : "Fuera de la carta").ancho(120)
                    .comoEstado(p -> p.disponible() ? Colores.exito() : Colores.secundario()))
            .vacia("La carta está vacía: agregá el primer producto a la derecha");
    private final JLabel resumen = new JLabel(" ");

    private final JTextField nombreProducto = new JTextField();
    private final JComboBox<Opcion<String>> tipoProducto = new JComboBox<>();
    private final JTextField precioProducto = Campos.soloDecimal(new JTextField());
    private final CampoImagen imagenProducto;
    private final JLabel errorProducto = Componentes.texto(" ");

    private final FormularioCombo formularioCombo;
    private final BotonesDeFila<Producto> botones;

    Carta(ApiCandy apiCandy, ApiCatalogos apiCatalogos, ApiImagenes apiImagenes) {
        super(new BorderLayout(Escala.BLOQUE, 0));
        this.apiCandy = apiCandy;
        this.apiImagenes = apiImagenes;
        this.imagenProducto = new CampoImagen(apiImagenes);
        this.formularioCombo = new FormularioCombo(apiCandy, apiImagenes, this::recargar);
        // Command: ver AccionDeFila.
        this.botones = new BotonesDeFila<>(this, tabla, this::recargar, new CambiarDisponibilidadProducto(apiCandy));
        JPanel acciones = Componentes.aLaDerecha(editar);
        botones.agregarA(acciones);
        JPanel centro = new JPanel(new BorderLayout(0, Escala.JUNTO));
        centro.add(Componentes.listado(null, resumen, acciones, tabla.conScroll()), BorderLayout.CENTER);
        centro.add(Componentes.nota("Los productos no se borran: se sacan de la carta. Uno que ya se vendió tiene "
                + "que seguir existiendo para que el ticket de esa venta diga qué se llevó. Cambiar un precio no "
                + "toca las ventas hechas: cada una guardó el precio que tenía."), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(altas(), BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        tabla.alDobleClic(this::editar);
        editar.addActionListener(e -> tabla.seleccionada().ifPresent(this::editar));
        habilitar();
        // Qué tipos se dan de alta sueltos lo dice el catálogo: el combo se arma abajo, con sus componentes.
        cargar(apiCatalogos::obtenerTiposProducto, tipos -> tipos.stream().filter(t -> !t.esCombo())
                .forEach(t -> tipoProducto.addItem(new Opcion<>(t.nombre(), etiqueta(t.nombre())))));
        recargar();
    }

    private void habilitar() {
        var elegido = tabla.seleccionada();
        editar.setEnabled(elegido.isPresent());
        botones.refrescar();
    }

    private JScrollPane altas() {
        JButton agregar = Componentes.primario(new JButton("Agregar a la carta"));
        agregar.addActionListener(e -> crearProducto());

        // Una sola alta a la vista: con las dos apiladas competían por el mismo costado.
        // Las pestañas miden lo que la más alta: cada tarjeta va arriba, no estirada en el hueco.
        JTabbedPane altas = new JTabbedPane();
        altas.addTab("Producto suelto", Componentes.arriba(Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Nuevo producto"))
                .obligatorio("Nombre", nombreProducto)
                .obligatorio("Tipo", tipoProducto)
                .obligatorio("Precio", precioProducto)
                .campo("Imagen", imagenProducto)
                .ancho(Componentes.aLaDerecha(agregar))
                .ancho(errorProducto))));
        altas.addTab("Combo", Componentes.arriba(formularioCombo));
        return Componentes.lateral(altas);
    }

    private void recargar() {
        cargar(() -> apiCandy.obtenerProductosCandy(true), productos -> {
            tabla.mostrar(productos);
            resumen.setText(cantidad(productos.size(), "producto", "productos") + " · "
                    + productos.stream().filter(Producto::disponible).count() + " a la venta");
            formularioCombo.pintarComponentes(productos.stream().filter(p -> !p.esCombo()).toList());
            habilitar();
        });
    }

    private void crearProducto() {
        Validacion v = new Validacion(errorProducto);
        String nombre = v.texto(nombreProducto, "Nombre", true);
        String tipo = v.elegido(tipoProducto, "Tipo");
        Double precio = v.decimal(precioProducto, "Precio", true);
        if (!v.ok()) return;
        PedidoProducto pedido = new PedidoProducto(nombre, tipo, precio, imagenProducto.url());
        Tarea.ejecutar(this, () -> apiCandy.crearProductoCandy(pedido), creado -> {
            avisar("Producto agregado");
            nombreProducto.setText("");
            precioProducto.setText("");
            imagenProducto.poner(null);
            recargar();
        }, v::mostrarError);
    }

    private void editar(Producto p) {
        new EdicionProducto(this, apiCandy, apiImagenes, p, this::recargar).abrir();
    }
}
