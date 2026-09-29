package ar.uade.cine.model.candy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Getter;

// Artículo o combo de la carta del candy; Experto: valida sus datos y R14, y Creador de sus ItemCombo.
// Validar acá y no en el gestor impide armar uno inválido, venga del gestor o de un test.
// GestorProductos se queda con lo que necesita la base: el nombre repetido, buscar los componentes
// y los combos que traen un suelto.
@Entity
@Getter
public class Producto {

    // El VARCHAR(60) de la tabla: pasado, MySQL rechaza el INSERT y el usuario vería un 500.
    private static final int LARGO_MAXIMO_DEL_NOMBRE = 60;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "combo_item", joinColumns = @JoinColumn(name = "combo_id"))
    private List<ItemCombo> componentes = new ArrayList<>();

    protected Producto() {
    }

    // Solo sueltos: un combo nace en armarCombo, que no lo deja existir sin declarar qué trae.
    public Producto(String nombre, TipoProducto tipo, Dinero precio) {
        if (tipo != null && tipo.esCombo()) {
            throw new IllegalArgumentException("Un combo se arma con armarCombo, para que declare qué trae");
        }
        this.nombre = nombreValido(nombre);
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de producto");
        }
        this.tipo = tipo;
        this.precio = precioValido(precio);
    }

    // Creador: el combo contiene sus ItemCombo, así que los crea él; fuera del paquete nadie arma uno.
    public static Producto armarCombo(String nombre, Dinero precio, Map<Producto, Integer> componentes) {
        Producto combo = new Producto();
        combo.nombre = nombreValido(nombre);
        combo.tipo = TipoProducto.COMBO;
        combo.precio = precioValido(precio);
        if (componentes == null || componentes.size() < 2) {
            throw new IllegalArgumentException("Un combo tiene que juntar al menos dos productos distintos");
        }
        componentes.forEach((producto, cantidad) -> combo.componentes.add(new ItemCombo(producto, cantidad)));
        if (!combo.saleMenosQueSuelto(precio)) {
            throw new IllegalArgumentException("El combo tiene que salir menos que sus componentes sueltos ($ "
                    + combo.getPrecioSuelto() + ")");
        }
        return combo;
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

    // Los componentes no cambian: se fijan al armar el combo. Si rechaza, no toca nada.
    public void editar(String nombre, Dinero precio) {
        String nuevoNombre = nombreValido(nombre);
        Dinero nuevoPrecio = precioValido(precio);
        if (esCombo() && !saleMenosQueSuelto(nuevoPrecio)) {
            throw new IllegalArgumentException(dejariaDeConvenir(nuevoNombre));
        }
        this.nombre = nuevoNombre;
        this.precio = nuevoPrecio;
    }

    // R14 del otro lado: abaratar un suelto puede dejar sin convenir a un combo que lo trae. El
    // combo no se entera solo de que cambió el precio de su componente; se lo pregunta el gestor.
    public void exigirQueSigaConviniendo() {
        if (esCombo() && !saleMenosQueSuelto(precio)) {
            throw new IllegalArgumentException(dejariaDeConvenir(nombre));
        }
    }

    public List<ItemCombo> getComponentes() {
        return new ArrayList<>(componentes);
    }

    public boolean esCombo() {
        return tipo.esCombo();
    }

    public Dinero getPrecioSuelto() {
        return Dinero.sumar(componentes.stream().map(ItemCombo::precioSuelto).toList());
    }

    public Dinero getAhorro() {
        return esCombo() ? getPrecioSuelto().menos(precio) : Dinero.CERO;
    }

    // R14: un combo que no sale menos que sus componentes sueltos no tiene por qué comprarse.
    private boolean saleMenosQueSuelto(Dinero precio) {
        return getPrecioSuelto().esMayorQue(precio);
    }

    private String dejariaDeConvenir(String nombre) {
        return "Con ese precio, el combo " + nombre + " dejaría de salir menos que sus componentes sueltos ($ "
                + getPrecioSuelto() + ")";
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_DEL_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede tener más de 60 caracteres");
        }
        return limpio;
    }

    private static Dinero precioValido(Dinero precio) {
        return Dinero.importeValido(precio, "precio");
    }

    @Override
    public String toString() {
        String detalle = componentes.isEmpty() ? "" : " " + componentes;
        String baja = disponible ? "" : " (sin stock)";
        return "[" + id + "] " + nombre + " - $" + precio + detalle + baja;
    }
}
