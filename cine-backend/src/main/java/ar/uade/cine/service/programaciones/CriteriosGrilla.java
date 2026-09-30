package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.funciones.validacion.ValidadorFuncion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.validacion.Regla;

// Pedido de grilla automática del encargado; Value Object: completa los defaults y se valida al nacer.
// Se valida al construirse: el planificador nunca ve unos criterios imposibles, y se queda solo
// con lo que necesita la base (que haya salas).
// Apertura y cierre no son una FranjaHoraria: el cierre 00:00 es el final del día, y una franja
// que termina a la medianoche empezaría antes de terminar.
public record CriteriosGrilla(LocalDate desde, int dias, LocalTime apertura, LocalTime cierre,
                              int cuantasPeliculas, Dinero precio, Version version,
                              Proyeccion proyeccion) {

    // La propuesta se arma entera en memoria, pase por pase: sin tope, un pedido de años la tumba.
    public static final int MAXIMO_DIAS = 31;

    // Los pases se reparten entre el elenco: con cuatro salas de 14 a 0 salen unos veinte por día, así
    // que pasadas las veinte películas a alguna le toca menos de un pase diario y no se sostiene en
    // cartel. El default son ocho.
    public static final int MAXIMO_PELICULAS = 20;

    public CriteriosGrilla {
        Regla.objeto(desde).obligatorio("Falta la fecha de inicio de la grilla");
        Regla.numero(dias).mayorQueCero("La grilla tiene que cubrir al menos un día")
                .entre(1, MAXIMO_DIAS, "La grilla no puede cubrir más de " + MAXIMO_DIAS + " días");
        Regla.numero(cuantasPeliculas).mayorQueCero("Hay que programar al menos una película")
                .entre(1, MAXIMO_PELICULAS, "La grilla no puede tener más de " + MAXIMO_PELICULAS + " películas");
        ValidadorFuncion.formato(version, proyeccion, precio);
        // Los pases arrancan en la apertura y de ahí en adelante: con segundos, ninguno sería programable.
        ValidadorFuncion.sinSegundos(apertura, "La hora de apertura");
        if (!desde.atTime(apertura).isBefore(cierreDe(desde, cierre))) {
            throw new DatoInvalido("El cine tiene que cerrar después de abrir");
        }
    }

    // Lo que el pedido no trae es una semana desde hoy, de 14 a 0, con 8 películas subtituladas en 2D.
    // Se completa y se valida en una sola construcción: el primer error es el del pedido, no el de un
    // default. hoy lo pasa quien tiene el reloj, igual que a las funciones que la grilla va a crear: no
    // empieza en el pasado ni termina a más de un año.
    public static CriteriosGrilla completando(LocalDate hoy, LocalDate desde, Integer dias, LocalTime apertura,
                                              LocalTime cierre, Integer cuantasPeliculas, Dinero precio,
                                              Version version, Proyeccion proyeccion) {
        LocalDate inicio = desde == null ? hoy : desde;
        // Antes daba 201 con cero funciones: los días pasados no tienen ningún pase posible (R20).
        if (inicio.isBefore(hoy)) {
            throw new DatoInvalido("La grilla no puede empezar en el pasado");
        }
        CriteriosGrilla criterios = new CriteriosGrilla(inicio,
                dias == null ? 7 : dias,
                apertura == null ? LocalTime.of(14, 0) : apertura,
                cierre == null ? LocalTime.MIDNIGHT : cierre,
                cuantasPeliculas == null ? 8 : cuantasPeliculas,
                precio,
                version == null ? Version.SUBTITULADA : version,
                proyeccion == null ? Proyeccion.DOS_D : proyeccion);
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

    // Un cierre 00:00 es el final del día, no su principio: el instante en que empieza el siguiente,
    // así entra el pase que termina justo a la medianoche. Estática porque el constructor compacto
    // la usa antes de que los campos tengan valor.
    private static LocalDateTime cierreDe(LocalDate fecha, LocalTime cierre) {
        return cierre.equals(LocalTime.MIDNIGHT) ? fecha.plusDays(1).atStartOfDay() : fecha.atTime(cierre);
    }
}
