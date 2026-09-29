package ar.uade.cine.model.salas;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Butaca de una sala; Experto en su código (A5) y en su estado, que vale para toda función (R9).
@Entity
@Getter
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Igual que el ON DELETE CASCADE del schema: borrar la sala se lleva sus butacas.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sala sala;

    private int fila;

    private int numero;

    @Enumerated(EnumType.STRING)
    private TipoAsiento tipo;

    @Enumerated(EnumType.STRING)
    private EstadoAsiento estado;

    protected Asiento() {
    }

    public Asiento(Sala sala, int fila, int numero, TipoAsiento tipo) {
        this.sala = sala;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
        this.estado = EstadoAsiento.HABILITADO;
    }

    // No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas.
    public int getSalaId() {
        return sala.getId();
    }

    public void marcarFueraDeServicio() {
        this.estado = EstadoAsiento.FUERA_DE_SERVICIO;
    }

    public void reponer() {
        this.estado = EstadoAsiento.HABILITADO;
    }

    // R9: es del asiento y no de la función, así que no se vende en ninguna.
    public boolean estaFueraDeServicio() {
        return estado == EstadoAsiento.FUERA_DE_SERVICIO;
    }

    public String getCodigo() {
        return codigoDe(fila, numero);
    }

    public static String codigoDe(int fila, int numero) {
        return (char) ('A' + fila - 1) + String.valueOf(numero);
    }

    public static Optional<Asiento> conCodigo(List<Asiento> asientos, String codigo) {
        String buscado = normalizarCodigo(codigo);
        return asientos.stream().filter(a -> a.getCodigo().equals(buscado)).findFirst();
    }

    // La butaca de un pedido (venta o bloqueo): el código puede faltar, y el mensaje es el mismo
    // en los dos caminos porque sale de acá.
    public static Asiento exigirConCodigo(List<Asiento> deLaSala, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DatoInvalido("Falta el código de una butaca");
        }
        return conCodigo(deLaSala, codigo)
                .orElseThrow(() -> new DatoInvalido(inexistente(codigo)));
    }

    // El mismo texto llegue la butaca en la ruta (404), en una venta, un bloqueo o el alta de la
    // sala (400). Sin nombrar la sala: quien pide ya sabe cuál es, y la lista puede no traerla.
    public static String inexistente(String codigo) {
        return "La butaca " + normalizarCodigo(codigo) + " no existe en la sala";
    }

    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }

    @Override
    public String toString() {
        String extra = tipo == TipoAsiento.ESTANDAR ? "" : " (" + tipo + ")";
        if (estaFueraDeServicio()) {
            extra += " FUERA DE SERVICIO";
        }
        return getCodigo() + extra;
    }
}
