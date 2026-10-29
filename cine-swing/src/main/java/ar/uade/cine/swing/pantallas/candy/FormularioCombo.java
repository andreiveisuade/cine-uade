package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.dto.candy.PedidoCombo;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.comun.CampoImagen;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import static ar.uade.cine.swing.comun.Formato.precio;

// El alta de un combo en la carta: nombre, precio y cuántos de cada producto suelto trae.
final class FormularioCombo extends Seccion {

    private final ApiCandy apiCandy;
    private final Runnable alGuardar;
    private final JTextField nombre = new JTextField();
    private final JTextField precioCombo = Campos.soloDecimal(new JTextField());
    private final JPanel filas = new JPanel(new GridBagLayout());
    private final Cantidades cantidades = new Cantidades();
    private final CampoImagen imagen;
    private final JLabel error = Componentes.texto(" ");

    FormularioCombo(ApiCandy apiCandy, ApiImagenes apiImagenes, Runnable alGuardar) {
        super(new BorderLayout());
        this.apiCandy = apiCandy;
        this.alGuardar = alGuardar;
        this.imagen = new CampoImagen(apiImagenes);
        JButton armar = Componentes.primario(new JButton("Armar combo"));
        armar.addActionListener(e -> armar());
        add(Componentes.conBorde(new Formulario()
                .ancho(Componentes.subtitulo("Armar combo"))
                .ancho(Componentes.nota("Al menos dos productos. El combo tiene que salir menos que sus "
                        + "componentes sueltos (R14): si no, no habría motivo para ofrecerlo."))
                .obligatorio("Nombre", nombre)
                .ancho(new JLabel("Qué trae (cantidad)"))
                .ancho(filas)
                .obligatorio("Precio del combo", precioCombo)
                .campo("Imagen", imagen)
                .ancho(Componentes.aLaDerecha(armar))
                .ancho(error)
                .cerrar()), BorderLayout.CENTER);
    }

    void pintarComponentes(List<Producto> sueltos) {
        filas.removeAll();
        cantidades.olvidar();
        if (sueltos.isEmpty()) {
            filas.add(Componentes.nota("Primero cargá productos sueltos"));
        }
        int fila = 0;
        for (Producto p : sueltos) {
            JLabel etiqueta = new JLabel(p.nombre() + "  · " + precio(p.precio()));
            if (!p.disponible()) etiqueta.setForeground(Colores.secundario());
            JSpinner spinner = cantidades.nueva(p.id());
            GridBagConstraints izquierda = new GridBagConstraints();
            izquierda.gridy = fila;
            izquierda.weightx = 1;
            izquierda.anchor = GridBagConstraints.WEST;
            izquierda.insets = new Insets(2, 0, 2, Escala.JUNTO);
            filas.add(etiqueta, izquierda);
            GridBagConstraints derecha = new GridBagConstraints();
            derecha.gridy = fila++;
            derecha.gridx = 1;
            filas.add(spinner, derecha);
        }
        filas.revalidate();
        filas.repaint();
    }

    // El mínimo de dos productos y R14 los valida el backend: su mensaje aparece abajo del formulario.
    private void armar() {
        Validacion v = new Validacion(error);
        String texto = v.texto(nombre, "Nombre", true);
        Double monto = v.decimal(precioCombo, "Precio del combo", true);
        v.alMencionar("combo", precioCombo);
        if (!v.ok()) return;
        PedidoCombo pedido = new PedidoCombo(texto, monto, cantidades.elegidas(), imagen.url());
        Tarea.ejecutar(this, () -> apiCandy.armarComboCandy(pedido), creado -> {
            avisar("Combo armado");
            nombre.setText("");
            precioCombo.setText("");
            imagen.poner(null);
            alGuardar.run();
        }, v::mostrarError);
    }
}
