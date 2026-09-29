package ar.uade.cine.model.ventas;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// La butaca que alguien está eligiendo, por función: dura MIENTRAS_ELIGE y es solo
// experiencia de usuario, la doble venta la impide el UNIQUE de entrada. La clave es el
// par función-butaca, así que la base no deja que dos sesiones la tengan a la vez.
// Nadie la instancia: se toma, renueva y suelta con consultas de BloqueoButacaRepository,
// porque cada una tiene que ser una sola sentencia atómica.
@Entity
@IdClass(BloqueoButaca.Clave.class)
public class BloqueoButaca {

    public record Clave(int funcion, int asiento) implements Serializable {
    }

    // CASCADE igual que en el schema: borrar la función o la sala no puede trabarse por
    // alguien que estaba mirando el mapa.
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcion_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Funcion funcion;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asiento asiento;

    @Column(nullable = false, length = 64)
    @Getter
    @Accessors(fluent = true)
    private String sesion;

    @Column(nullable = false)
    private LocalDateTime venceEn;

    protected BloqueoButaca() {
    }

    // No inicializa el proxy: el id ya viene en la fila.
    public int asientoId() {
        return asiento.getId();
    }
}
