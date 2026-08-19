package ar.uade.cine.service.funciones;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que una sala ya tiene tomado, leído una sola vez.
 *
 * <p>Existe por una razón de costo y no de modelo. {@link GestorFunciones#superpuestaEn}
 * resuelve R3 perfectamente para una consulta suelta —el alta de una función, una fecha de
 * una grilla— pero cada llamada relee las funciones de la sala y la duración de cada
 * película. Al planificador eso no le alcanza: prueba cientos de horarios por corrida, y
 * repetir esas lecturas en cada intento llevaba una propuesta de una semana a más de veinte
 * segundos contra MySQL.
 *
 * <p><strong>La regla no se mueve de lugar.</strong> Quién decide si dos rangos se pisan
 * —y que la limpieza cuenta— sigue siendo {@code GestorFunciones}, que es el que arma esta
 * agenda con los tramos ya calculados. Acá solo se comparan números que otro resolvió. Si
 * mañana R3 cambia, cambia en un solo lugar.
 */
public final class AgendaDeSala {

    /** Un rato en que la sala está tomada: la función más su limpieza. */
    public record Tramo(LocalDateTime inicio, LocalDateTime fin) {
    }

    private final int minutosLimpieza;
    private final List<Tramo> tomados;

    AgendaDeSala(int minutosLimpieza, List<Tramo> tomados) {
        this.minutosLimpieza = minutosLimpieza;
        this.tomados = tomados;
    }

    /**
     * Si una función que va de {@code inicio} a {@code fin} se pisa con algo ya tomado.
     *
     * <p>Al rango que se consulta se le suma la limpieza, igual que ya la tienen sumada los
     * tramos guardados: la función nueva también deja la sala sucia para la que venga
     * después. Dos rangos se pisan si cada uno empieza antes de que termine el otro.
     */
    public boolean chocaEn(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime finConLimpieza = fin.plusMinutes(minutosLimpieza);
        for (Tramo tomado : tomados) {
            if (inicio.isBefore(tomado.fin()) && tomado.inicio().isBefore(finConLimpieza)) {
                return true;
            }
        }
        return false;
    }
}
