package ar.uade.cine.infrastructure.reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * De dónde sale "ahora". Es un puerto como los demás de esta capa: del otro lado hay un
 * sistema —el reloj de la máquina— que el negocio no controla.
 *
 * <p>Los gestores lo reciben por constructor en vez de llamar a {@code LocalDateTime.now()},
 * por el mismo motivo por el que {@link ar.uade.cine.model.ventas.Reserva#estaVencida}
 * recibe el instante: las reglas que dependen del tiempo —R17, R19, la espera entre
 * importaciones— no se pueden probar contra el reloj de la pared. Con {@code now()} suelto
 * en el código, un test que arma una función para el 20 de agosto pasa hasta el 20 de
 * agosto y se pone rojo al día siguiente, aunque el sistema haga exactamente lo que debe.
 *
 * <p>En producción es {@code LocalDateTime::now}, elegido en {@code Adaptadores}. En los
 * tests es uno que se mueve a mano.
 */
@FunctionalInterface
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
