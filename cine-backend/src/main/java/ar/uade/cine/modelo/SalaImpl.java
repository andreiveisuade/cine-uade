package ar.uade.cine.modelo;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Sala;

public class SalaImpl implements Sala {

    private int id;
    private String nombre;
    private TipoSala tipo;
    private final List<Integer> butacasPorFila = new ArrayList<>();

    public SalaImpl(String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.butacasPorFila.addAll(butacasPorFila);
    }

    public SalaImpl(int id, String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        this(nombre, tipo, butacasPorFila);
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public TipoSala getTipo() {
        return tipo;
    }

    @Override
    public List<Integer> getButacasPorFila() {
        return new ArrayList<>(butacasPorFila);
    }

    @Override
    public int getFilas() {
        return butacasPorFila.size();
    }

    @Override
    public int getCapacidadSala() {
        return butacasPorFila.stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo + " - " + getFilas()
                + " filas, " + getCapacidadSala() + " butacas";
    }
}
