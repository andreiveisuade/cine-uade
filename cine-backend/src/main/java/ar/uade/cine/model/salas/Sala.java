package ar.uade.cine.model.salas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import ar.uade.cine.model.salas.validacion.ValidadorSala;

// Sala de proyección (R2); Experto en sus datos y Creador de sus butacas, que genera solo en el alta.
@Entity
@Getter
public class Sala {

    public static final int LIMPIEZA_POR_DEFECTO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoSala tipo;

    private int minutosLimpieza;

    protected Sala() {
    }

    public Sala(String nombre, TipoSala tipo, int minutosLimpieza) {
        editar(nombre, tipo, minutosLimpieza);
    }

    // No toca las butacas: rehacerlas dejaría entradas vendidas apuntando a asientos inexistentes.
    // Valida lo mismo que el alta, y todo antes de asignar nada: el gestor solo chequea lo que
    // necesita la base (nombre repetido, funciones programadas).
    public void editar(String nombre, TipoSala tipo, int minutosLimpieza) {
        String nuevoNombre = ValidadorSala.nombre(nombre);
        ValidadorSala.tipo(tipo);
        ValidadorSala.limpieza(minutosLimpieza);
        this.nombre = nuevoNombre;
        this.tipo = tipo;
        this.minutosLimpieza = minutosLimpieza;
    }

    // Creadora de sus butacas porque las contiene, y por eso dueña de la distribución. Solo en el
    // alta: editar no las toca. Las especiales llegan por tipo, como las listas del pedido, para que
    // una butaca repetida en dos se pueda rechazar.
    public List<Asiento> generarAsientos(List<Integer> butacasPorFila,
                                         Map<TipoAsiento, List<String>> especiales) {
        ValidadorSala.distribucion(butacasPorFila);
        Map<String, TipoAsiento> porCodigo = ValidadorSala.especiales(butacasPorFila, especiales);
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= butacasPorFila.size(); fila++) {
            for (int numero = 1; numero <= butacasPorFila.get(fila - 1); numero++) {
                TipoAsiento tipo = porCodigo.getOrDefault(Asiento.codigoDe(fila, numero), TipoAsiento.ESTANDAR);
                asientos.add(new Asiento(this, fila, numero, tipo));
            }
        }
        return asientos;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo;
    }
}
