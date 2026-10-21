package ar.uade.cine.model.candy;

import java.util.List;

import org.hibernate.annotations.DiscriminatorFormula;

import ar.uade.cine.model.candy.validacion.ValidadorProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.imagenes.ImagenUrl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AccessLevel;
import lombok.Getter;

// Artículo de la carta del candy, suelto o combo; Experto en su disponibilidad y en editar nombre y precio.
// Patrón Composite (Component): ProductoSuelto es la hoja, Combo el grupo; cada uno contesta lo suyo sin esCombo().
// Discriminador por fórmula, como Usuario: la columna tipo ya existía y cuatro rubros caen en dos clases.
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when tipo = 'COMBO' then 'COMBO' else 'SUELTO' end")
@Getter
public abstract class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoProducto tipo;

    private Dinero precio;

    @Getter(AccessLevel.NONE)
    private boolean disponible = true;

    private String imagenUrl;

    protected Producto() {
    }

    // Valida todo antes de asignar; el orden (nombre, tipo, precio) es el de los mensajes de siempre.
    protected Producto(String nombre, TipoProducto tipo, Dinero precio) {
        String nombreValido = ValidadorProducto.nombre(nombre);
        TipoProducto tipoValido = ValidadorProducto.tipo(tipo);
        Dinero precioValido = ValidadorProducto.precio(precio);
        this.nombre = nombreValido;
        this.tipo = tipoValido;
        this.precio = precioValido;
    }

    // Valida sin tocar ningún producto: uno ya modificado haría flush contra el UNIQUE al buscar el nombre repetido.
    // Devuelve el nombre recortado, el mismo que se guarda.
    public static String validarNombreYPrecio(String nombre, Dinero precio) {
        String nombreValido = ValidadorProducto.nombre(nombre);
        ValidadorProducto.precio(precio);
        return nombreValido;
    }

    // Lo que costaría comprar por separado lo que trae: el suelto es él mismo, el combo suma sus componentes.
    public abstract Dinero getPrecioSuelto();

    public abstract Dinero getAhorro();

    // Lectura transparente del Composite: la hoja devuelve lista vacía; armar solo existe en Combo.armar.
    public List<ItemCombo> getComponentes() {
        return List.of();
    }

    // Dato para la vista y para la regla de que un combo no trae otro; ningún cálculo pregunta esto.
    public boolean esCombo() {
        return tipo.esCombo();
    }

    public boolean estaDisponible() {
        return disponible;
    }

    // Sin stock no se borra: vive en compras viejas. Sale de la venta y vuelve cuando hay.
    public void sacarDeLaVenta() {
        disponible = false;
    }

    public void volverALaVenta() {
        disponible = true;
    }

    // Los componentes se fijan al armar. Valida todo antes de asignar.
    public void editar(String nombre, Dinero precio) {
        String nuevoNombre = ValidadorProducto.nombre(nombre);
        Dinero nuevoPrecio = ValidadorProducto.precio(precio);
        validarEdicion(nuevoNombre, nuevoPrecio);
        this.nombre = nuevoNombre;
        this.precio = nuevoPrecio;
    }

    public void cambiarImagen(ImagenUrl imagen) {
        this.imagenUrl = imagen.valor();
    }

    // Lo que cada clase agrega a la edición; el suelto no pide nada más, el combo pide R14.
    protected void validarEdicion(String nombre, Dinero precio) {
    }

    @Override
    public String toString() {
        List<ItemCombo> componentes = getComponentes();
        String detalle = componentes.isEmpty() ? "" : " " + componentes;
        String baja = disponible ? "" : " (sin stock)";
        return "[" + id + "] " + nombre + " - $" + precio + detalle + baja;
    }
}
