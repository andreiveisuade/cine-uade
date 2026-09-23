package ar.uade.cine.model.ventas;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.salas.Asiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Una butaca vendida dentro de una reserva. No se consulta sola: vive con su reserva.
 *
 * <p>El asiento entra como {@code @ManyToOne}, a diferencia del resto del dominio: cada
 * vez que se lee una entrada hace falta el código de la butaca ("B7") para el ticket.
 */
@Entity
@Table(uniqueConstraints = {
        // R4: lo que de verdad impide vender la misma butaca dos veces en la misma función.
        // Va acá además del schema para que los tests sobre H2 también lo tengan.
        @UniqueConstraint(columnNames = {"funcion_id", "asiento_id"}),
        @UniqueConstraint(columnNames = {"reserva_id", "asiento_id"})
})
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    /**
     * La función de la reserva, copiada acá para que exista el {@code UNIQUE (funcion_id,
     * asiento_id)}. Se pone en NULL al liberar (R6): la butaca vuelve a la venta y la
     * entrada sigue existiendo para contar qué tenía la reserva.
     */
    @Column(name = "funcion_id")
    private Integer funcionId;

    /** Por persona, y por eso acá y no en la reserva: en una de cuatro puede haber dos generales y un jubilado. */
    @Enumerated(EnumType.STRING)
    private TipoTarifa tarifa;

    /**
     * El precio de lista con la tarifa aplicada, congelado al reservar: si mañana sube el
     * precio, el ticket sigue diciendo lo que se pagó. El descuento vive en el {@link Pago}.
     */
    private Dinero precio;

    protected Entrada() {
    }

    public Entrada(Asiento asiento, TipoTarifa tarifa, Dinero precio) {
        this.asiento = asiento;
        this.tarifa = tarifa;
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public int asientoId() {
        return asiento.getId();
    }

    /** El código de la butaca —"B7"— que va impreso en el ticket. */
    public String codigoAsiento() {
        return asiento.getCodigo();
    }

    public TipoTarifa tarifa() {
        return tarifa;
    }

    public Dinero precio() {
        return precio;
    }

    /** La ata a su función, que es lo que la hace ocupar la butaca. */
    void ocupar(int funcionId) {
        this.funcionId = funcionId;
    }

    /** Devuelve la butaca a la venta sin borrar la entrada. */
    void liberar() {
        this.funcionId = null;
    }

    @Override
    public String toString() {
        return tarifa == TipoTarifa.GENERAL ? codigoAsiento() : codigoAsiento() + " (" + tarifa + ")";
    }
}
