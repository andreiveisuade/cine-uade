package ar.uade.cine.model.candy;

import java.util.List;

import org.hibernate.annotations.DiscriminatorFormula;

import ar.uade.cine.model.candy.validacion.ValidadorProducto;
import ar.uade.cine.model.dinero.Dinero;
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
// Patrón Composite (Component): antes una sola clase hacía de hoja y de grupo, y getAhorro, R14 y toString
// preguntaban esCombo(). Ahora quien cobra (ItemCompra) o muestra (VistasCandy) le habla a un Producto y
// cada subclase contesta lo suyo: ProductoSuelto es la hoja, Combo el grupo. Los datos los validan
// ValidadorProducto y ValidadorCombo al construirse o editarse: así no se arma uno inválido.
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

    // Nombre y precio sin tocar ningún producto: GestorProductos los valida antes de buscar el nombre
    // repetido, y uno ya modificado haría flush contra el UNIQUE en esa consulta. Devuelve el nombre recortado,
    // el mismo valor que se guarda y que la base compara.
    public static String validarNombreYPrecio(String nombre, Dinero precio) {
        String nombreValido = ValidadorProducto.nombre(nombre);
        ValidadorProducto.precio(precio);
        return nombreValido;
    }

    // Lo que costaría comprar por separado lo que trae: el suelto es él mismo, el combo suma sus componentes.
    public abstract Dinero getPrecioSuelto();

    public abstract Dinero getAhorro();

    // Lectura transparente del Composite: la hoja no tiene hijos y lo dice con una lista vacía. Armar o
    // agregar no está acá (obligaría al suelto a un método que no puede cumplir): solo Combo.armar.
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

    // Los componentes no cambian: se fijan al armar el combo. Valida todo antes de asignar: si rechaza,
    // no toca nada.
    public void editar(String nombre, Dinero precio) {
        String nuevoNombre = ValidadorProducto.nombre(nombre);
        Dinero nuevoPrecio = ValidadorProducto.precio(precio);
        validarEdicion(nuevoNombre, nuevoPrecio);
        this.nombre = nuevoNombre;
        this.precio = nuevoPrecio;
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
