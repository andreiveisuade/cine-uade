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

/**
 * Una butaca vendida dentro de una reserva. No se consulta sola: se guarda y se lee
 * siempre junto con su reserva, que es su dueña.
 *
 * <p>Era un {@code record}, y dejó de serlo porque la tabla {@code entrada} tiene una clave
 * propia que hay que mapear —y un record no puede ser una entidad—. Los accesores conservan
 * los nombres de entonces ({@code precio()}, {@code tarifa()}) para que el resto del sistema
 * la siga leyendo igual.
 *
 * <p>El asiento sí entra como {@code @ManyToOne}, a diferencia de las referencias por id del
 * resto del dominio: el ticket necesita el código de la butaca —"B7"— y ese dato vive en el
 * asiento. Antes lo resolvía un JOIN escrito a mano en el DAO que traía la fila y el número
 * para rearmar el código; con la relación mapeada, eso lo hace Hibernate.
 */
@Entity
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    /**
     * La función de la reserva, copiada acá. No es un dato nuevo: es lo que hace posible el
     * {@code UNIQUE (funcion_id, asiento_id)} que impide vender la misma butaca dos veces en
     * la misma función.
     *
     * <p>Se pone en NULL al cancelar (R6): la fila deja de participar del UNIQUE, así que la
     * butaca vuelve a la venta, pero la entrada sigue existiendo con su precio y su tarifa
     * para que la reserva cancelada pueda contar qué tenía.
     */
    @Column(name = "funcion_id")
    private Integer funcionId;

    /**
     * Quién la compra. Es por persona, y por eso está acá y no en la reserva: en una reserva
     * de cuatro puede haber dos generales, un menor y un jubilado.
     */
    @Enumerated(EnumType.STRING)
    private TipoTarifa tarifa;

    /**
     * El precio <strong>de lista</strong> de esta butaca, ya con la tarifa aplicada,
     * congelado al reservar: si mañana sube el precio de la función, el ticket ya emitido
     * tiene que seguir diciendo lo que se pagó. Ninguna promoción lo toca: el descuento se
     * calcula sobre el total y vive en el {@link Pago}.
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
