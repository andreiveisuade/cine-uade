package ar.uade.cine.model.candy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.candy.validacion.ValidadorCombo;
import ar.uade.cine.model.candy.validacion.ValidadorProducto;
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

// Artículo o combo de la carta del candy; Experto en su disponibilidad y R14, y Creador de sus ItemCombo.
// Los datos los validan ValidadorProducto y ValidadorCombo, que llama Producto al construirse o editarse:
// así no se arma uno inválido, venga del gestor o de un test. GestorProductos se queda con lo que
// necesita la base: el nombre repetido, buscar los componentes y los combos que traen un suelto.
@Entity
@Getter
public class Producto {

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
        ValidadorProducto.exigirQueNoSeaCombo(tipo);
        String nombreValido = ValidadorProducto.nombre(nombre);
        TipoProducto tipoValido = ValidadorProducto.tipo(tipo);
        Dinero precioValido = ValidadorProducto.precio(precio);
        this.nombre = nombreValido;
        this.tipo = tipoValido;
        this.precio = precioValido;
    }

    // Creador: el combo contiene sus ItemCombo, así que los crea él y cada uno valida su componente. R14
    // va al final porque el precio suelto sale de los componentes ya armados.
    public static Producto armarCombo(String nombre, Dinero precio, Map<Producto, Integer> componentes) {
        Producto combo = new Producto();
        combo.nombre = ValidadorProducto.nombre(nombre);
        combo.tipo = TipoProducto.COMBO;
        combo.precio = ValidadorProducto.precio(precio);
        ValidadorCombo.exigirComponentes(componentes);
        componentes.forEach((producto, cantidad) -> combo.componentes.add(new ItemCombo(producto, cantidad)));
        ValidadorCombo.exigirQueConvenga(combo, combo.precio);
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

    // Los componentes no cambian: se fijan al armar el combo. Valida todo antes de asignar: si rechaza,
    // no toca nada.
    public void editar(String nombre, Dinero precio) {
        String nuevoNombre = ValidadorProducto.nombre(nombre);
        Dinero nuevoPrecio = ValidadorProducto.precio(precio);
        ValidadorProducto.exigirQueSigaConviniendo(this, nuevoNombre, nuevoPrecio);
        this.nombre = nuevoNombre;
        this.precio = nuevoPrecio;
    }

    // R14 del otro lado: abaratar un suelto puede dejar sin convenir a un combo que lo trae. El
    // combo no se entera solo de que cambió el precio de su componente; se lo pregunta el gestor.
    public void exigirQueSigaConviniendo() {
        ValidadorProducto.exigirQueSigaConviniendo(this, nombre, precio);
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

    @Override
    public String toString() {
        String detalle = componentes.isEmpty() ? "" : " " + componentes;
        String baja = disponible ? "" : " (sin stock)";
        return "[" + id + "] " + nombre + " - $" + precio + detalle + baja;
    }
}
