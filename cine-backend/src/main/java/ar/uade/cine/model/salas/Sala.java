package ar.uade.cine.model.salas;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Sala {

    public static final int LIMPIEZA_POR_DEFECTO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoSala tipo;

    private int minutosLimpieza;

    @OneToMany(mappedBy = "sala")
    private List<Asiento> asientos = new ArrayList<>();

    protected Sala() {
    }

    public Sala(String nombre, TipoSala tipo, int minutosLimpieza) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.minutosLimpieza = minutosLimpieza;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    public int getMinutosLimpieza() {
        return minutosLimpieza;
    }

    public List<Asiento> getAsientos() {
        return asientos;
    }

    // No toca las butacas: rehacerlas dejaría entradas vendidas apuntando a asientos inexistentes.
    public void editar(String nombre, TipoSala tipo, int minutosLimpieza) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.minutosLimpieza = minutosLimpieza;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo;
    }
}
