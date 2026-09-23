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

@Entity
@Table(uniqueConstraints = {
        // R4: impide vender la misma butaca dos veces por función. Acá también para H2.
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

    // Copiada de la reserva para el UNIQUE de R4. NULL al liberar (R6): la butaca vuelve a la venta.
    @Column(name = "funcion_id")
    private Integer funcionId;

    @Enumerated(EnumType.STRING)
    private TipoTarifa tarifa;

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

    public String codigoAsiento() {
        return asiento.getCodigo();
    }

    public TipoTarifa tarifa() {
        return tarifa;
    }

    public Dinero precio() {
        return precio;
    }

    void ocupar(int funcionId) {
        this.funcionId = funcionId;
    }

    void liberar() {
        this.funcionId = null;
    }

    @Override
    public String toString() {
        return tarifa == TipoTarifa.GENERAL ? codigoAsiento() : codigoAsiento() + " (" + tarifa + ")";
    }
}
