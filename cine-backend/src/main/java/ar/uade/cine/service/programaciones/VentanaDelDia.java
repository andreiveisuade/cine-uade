package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.funciones.Funcion;

// Desde cuándo y hasta cuándo se puede programar cada día de la grilla; Fabricación pura con el reloj leído una vez.
// SRP: la comparten reparto e indicadores, así la ocupación se mide contra lo que el reparto puede usar.
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

    // R20: hoy arranca en el primer intento que no pasó; en un día pasado da el cierre o más: nada que repartir.
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
