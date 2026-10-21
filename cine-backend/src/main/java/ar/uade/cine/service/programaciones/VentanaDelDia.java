package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.funciones.Funcion;

// Desde cuándo y hasta cuándo se puede programar cada día de la grilla; Fabricación pura con el reloj leído una vez.
// SRP: la comparten el reparto y los indicadores, para que la ocupación se mida contra lo que el reparto
// puede usar de verdad.
final class VentanaDelDia {

    static final int MINUTOS_ENTRE_INTENTOS = 30;

    private final CriteriosGrilla criterios;
    private final LocalDateTime ahora;

    VentanaDelDia(CriteriosGrilla criterios, LocalDateTime ahora) {
        this.criterios = criterios;
        this.ahora = ahora;
    }

    List<LocalDate> fechas() {
        return criterios.desde().datesUntil(criterios.desde().plusDays(criterios.dias())).toList();
    }

    // R20: hoy arranca en el primer intento que todavía no pasó; en un día que ya pasó da el
    // cierre o más, sin nada por repartir.
    LocalDateTime primerIntento(LocalDate fecha) {
        LocalDateTime momento = fecha.atTime(criterios.apertura());
        LocalDateTime limite = cierre(fecha);
        while (momento.isBefore(limite) && Funcion.yaPaso(momento, ahora)) {
            momento = momento.plusMinutes(MINUTOS_ENTRE_INTENTOS);
        }
        return momento;
    }

    LocalDateTime cierre(LocalDate fecha) {
        return criterios.cierreDe(fecha);
    }
}
