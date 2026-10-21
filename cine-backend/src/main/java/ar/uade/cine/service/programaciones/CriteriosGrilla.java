package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.funciones.validacion.ValidadorFuncion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.validacion.Regla;

// Pedido de grilla automática del encargado; Value Object: completa los defaults y se valida al nacer.
// Se valida al construirse: el planificador nunca ve criterios imposibles.
// Apertura y cierre no son una FranjaHoraria: el cierre 00:00 es el final del día.
public record CriteriosGrilla(LocalDate desde, int dias, LocalTime apertura, LocalTime cierre,
                              int cuantasPeliculas, FormatoDeFuncion formato) {

    // La propuesta se arma entera en memoria, pase por pase: sin tope, un pedido de años la tumba.
    public static final int MAXIMO_DIAS = 31;

    // Con cuatro salas salen ~20 pases por día: pasadas 20 películas, a alguna le toca menos de uno. Default: ocho.
    public static final int MAXIMO_PELICULAS = 20;

    public CriteriosGrilla {
        Regla.objeto(desde).obligatorio("Falta la fecha de inicio de la grilla");
        Regla.numero(dias).mayorQueCero("La grilla tiene que cubrir al menos un día")
                .entre(1, MAXIMO_DIAS, "La grilla no puede cubrir más de " + MAXIMO_DIAS + " días");
        Regla.numero(cuantasPeliculas).mayorQueCero("Hay que programar al menos una película")
                .entre(1, MAXIMO_PELICULAS, "La grilla no puede tener más de " + MAXIMO_PELICULAS + " películas");
        // Los pases arrancan en la apertura y de ahí en adelante: con segundos, ninguno sería programable.
        ValidadorFuncion.sinSegundos(apertura, "La hora de apertura");
        if (!desde.atTime(apertura).isBefore(cierreDe(desde, cierre))) {
            throw new DatoInvalido("El cine tiene que cerrar después de abrir");
        }
    }

    // Sin lo que el pedido no trae: una semana desde hoy, de 14 a 0, con 8 películas subtituladas en 2D.
    // Completa y valida en una construcción; hoy lo pasa quien tiene el reloj: no empieza en el pasado ni pasa de un año.
    public static CriteriosGrilla completando(LocalDate hoy, LocalDate desde, Integer dias, LocalTime apertura,
                                              LocalTime cierre, Integer cuantasPeliculas, Dinero precio,
                                              Version version, Proyeccion proyeccion) {
        LocalDate inicio = desde == null ? hoy : desde;
        // R20: los días pasados no tienen ningún pase posible
        if (inicio.isBefore(hoy)) {
            throw new DatoInvalido("La grilla no puede empezar en el pasado");
        }
        CriteriosGrilla criterios = new CriteriosGrilla(inicio,
                dias == null ? 7 : dias,
                apertura == null ? LocalTime.of(14, 0) : apertura,
                cierre == null ? LocalTime.MIDNIGHT : cierre,
                cuantasPeliculas == null ? 8 : cuantasPeliculas,
                new FormatoDeFuncion(version == null ? Version.SUBTITULADA : version,
                        proyeccion == null ? Proyeccion.DOS_D : proyeccion, precio));
        ValidadorFuncion.dentroDelHorizonte(criterios.periodo().hasta(), hoy,
                "La grilla tiene que terminar dentro del próximo año");
        return criterios;
    }

    // Los días que cubre, el primero y el último incluidos.
    public Periodo periodo() {
        return new Periodo(desde, desde.plusDays(dias - 1L));
    }

    public LocalDateTime cierreDe(LocalDate fecha) {
        return cierreDe(fecha, cierre);
    }

    // Un cierre 00:00 es el instante en que empieza el día siguiente, así entra el pase que termina a medianoche.
    // Estática: el constructor compacto la usa antes de que los campos tengan valor.
    private static LocalDateTime cierreDe(LocalDate fecha, LocalTime cierre) {
        return cierre.equals(LocalTime.MIDNIGHT) ? fecha.plusDays(1).atStartOfDay() : fecha.atTime(cierre);
    }
}
