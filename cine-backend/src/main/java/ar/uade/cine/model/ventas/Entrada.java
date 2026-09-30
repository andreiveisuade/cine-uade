package ar.uade.cine.model.ventas;

import java.util.Objects;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.validacion.ValidadorEntrada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.experimental.Accessors;

// Butaca vendida con tarifa y precio fijos; su funcion_id sostiene el UNIQUE de R4 y se vacía al soltar (R6).
@Entity
@Table(uniqueConstraints = {
        // R4: impide vender la misma butaca dos veces por función. Acá también para H2.
        @UniqueConstraint(columnNames = {"funcion_id", "asiento_id"}),
        @UniqueConstraint(columnNames = {"reserva_id", "asiento_id"})
})
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private int id;

    // EAGER a propósito, como las colecciones: el código de la butaca lo leen las vistas,
    // ya fuera de la transacción.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    // Copiada de la reserva para el UNIQUE de R4. NULL al liberar (R6): la butaca vuelve a la venta.
    @Column(name = "funcion_id")
    private Integer funcionId;

    @Enumerated(EnumType.STRING)
    @Getter
    @Accessors(fluent = true)
    private TipoTarifa tarifa;

    @Getter
    @Accessors(fluent = true)
    private Dinero precio;

    protected Entrada() {
    }

    // El precio queda fijo al vender: si después cambia el de la función, lo vendido no cambia. Llega el de
    // la butaca con tarifa general (Funcion#precioDe) y la entrada le aplica la suya: la conoce ella.
    // Una butaca pedida sin tarifa va a la general: es el default del modelo, no de quien arma el pedido.
    public Entrada(Asiento asiento, TipoTarifa tarifa, Dinero precioDeLaButaca) {
        ValidadorEntrada.validar(asiento);
        TipoTarifa vendida = Objects.requireNonNullElse(tarifa, TipoTarifa.GENERAL);
        this.asiento = asiento;
        this.tarifa = vendida;
        this.precio = vendida.aplicarA(precioDeLaButaca);
    }

    public int asientoId() {
        return asiento.getId();
    }

    public String codigoAsiento() {
        return asiento.getCodigo();
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
