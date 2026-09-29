package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;

// El diálogo que cambia nombre y precio de un producto de la carta, lo único que el backend deja editar.
/**
 * Con un campo mal, el diálogo vuelve a abrirse con el motivo en vez de cerrarse y perder lo tipeado; también si el que
 * lo rechaza es el backend.
 */
final class EdicionProducto {

    private final Component origen;
    private final ApiCandy apiCandy;
    private final Producto producto;
    private final Runnable alGuardar;
    private final JTextField nombre;
    private final JTextField precio;
    private final JLabel error = Componentes.texto(" ");
    private final Componentes.Formulario formulario;

    EdicionProducto(Component origen, ApiCandy apiCandy, Producto producto, Runnable alGuardar) {
        this.origen = origen;
        this.apiCandy = apiCandy;
        this.producto = producto;
        this.alGuardar = alGuardar;
        nombre = new JTextField(producto.nombre(), 20);
        precio = Campos.soloDecimal(new JTextField(producto.precio() % 1 == 0
                ? String.valueOf((long) producto.precio()) : String.valueOf(producto.precio())));
        formulario = new Componentes.Formulario()
                .obligatorio("Nombre", nombre)
                .obligatorio("Precio", precio);
        if (producto.esCombo()) {
            formulario.ancho(Componentes.nota("Trae " + PantallaCandy.componentesDe(producto)
                    + ". Los componentes se fijan al armarlo."));
        }
        formulario.ancho(error);
    }

    void abrir() {
        abrir(null);
    }

    private void abrir(ErrorApi rechazo) {
        if (rechazo != null) {
            Validacion v = new Validacion(error);
            v.texto(nombre, "Nombre", true);
            v.decimal(precio, "Precio", true);
            v.mostrarError(rechazo);
        }
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(origen, formulario, "Editar " + producto.nombre(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;
            Validacion v = new Validacion(error);
            String nuevoNombre = v.texto(nombre, "Nombre", true);
            Double nuevoPrecio = v.decimal(precio, "Precio", true);
            if (v.ok()) {
                guardar(v, nuevoNombre, nuevoPrecio);
                return;
            }
        }
    }

    private void guardar(Validacion v, String nuevoNombre, Double nuevoPrecio) {
        Tarea.ejecutar(origen, () -> apiCandy.editarProductoCandy(producto.id(), nuevoNombre, nuevoPrecio), editado -> {
            Mensajes.exito(origen, "Producto actualizado");
            alGuardar.run();
        }, e -> {
            // Por la Validacion y no por el código: un nombre repetido (409) también se corrige acá.
            if (v.esDelFormulario(e)) abrir(e);
            else Mensajes.error(origen, e);
        });
    }
}
