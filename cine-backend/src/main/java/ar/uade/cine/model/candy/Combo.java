package ar.uade.cine.model.candy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.candy.validacion.ValidadorCombo;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

// Varios productos a un precio menor que por separado (R14); Experto en su ahorro y Creador de sus ItemCombo.
// Patrón Composite: precio suelto = suma de sus ItemCombo; que no anide combos lo prohíbe ValidadorCombo, no el diseño.
@Entity
@DiscriminatorValue("COMBO")
public class Combo extends Producto {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "combo_item", joinColumns = @JoinColumn(name = "combo_id"))
    private List<ItemCombo> componentes = new ArrayList<>();

    protected Combo() {
    }

    private Combo(String nombre, Dinero precio) {
        super(nombre, TipoProducto.COMBO, precio);
    }

    // Creador: crea sus ItemCombo; R14 va al final porque el precio suelto sale de los componentes ya armados.
    public static Combo armar(String nombre, Dinero precio, Map<Producto, Integer> componentes) {
        Combo combo = new Combo(nombre, precio);
        ValidadorCombo.exigirComponentes(componentes);
        componentes.forEach((producto, cantidad) -> combo.componentes.add(new ItemCombo(producto, cantidad)));
        ValidadorCombo.exigirQueConvenga(combo, combo.getPrecio());
        return combo;
    }

    @Override
    public List<ItemCombo> getComponentes() {
        return new ArrayList<>(componentes);
    }

    @Override
    public Dinero getPrecioSuelto() {
        return Dinero.sumar(componentes.stream().map(ItemCombo::precioSuelto).toList());
    }

    @Override
    public Dinero getAhorro() {
        return getPrecioSuelto().menos(getPrecio());
    }

    // R14 al editar; solo un combo llega acá
    @Override
    protected void validarEdicion(String nombre, Dinero precio) {
        ValidadorCombo.exigirQueSigaConviniendo(this, nombre, precio);
    }

    // R14: abaratar un suelto puede dejar sin convenir a un combo que lo trae; se lo pregunta el gestor.
    public void exigirQueSigaConviniendo() {
        ValidadorCombo.exigirQueSigaConviniendo(this, getNombre(), getPrecio());
    }
}
