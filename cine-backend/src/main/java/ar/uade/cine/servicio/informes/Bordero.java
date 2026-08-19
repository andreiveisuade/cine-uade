package ar.uade.cine.servicio.informes;

import java.time.LocalDateTime;
import java.util.Map;

import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * El borderó de una función: cuántas entradas se vendieron y a qué valor. Es lo que el
 * cine declara al INCAA por cada proyección.
 *
 * <p>Existe aparte del {@link Arqueo} porque el corte es otro y no se puede derivar uno
 * del otro. El arqueo cierra la caja de un <em>día</em> y mira los cobros; el borderó
 * declara una <em>función</em>, y las entradas de una función se venden a lo largo de
 * varios días. Sumar el arqueo del día de la proyección daría cualquier otra cosa: mezcla
 * las funciones de la noche y deja afuera lo que se vendió en la semana.
 *
 * <p>Las tres cifras de plata van separadas y no resumidas en una porque cuentan cosas
 * distintas: {@code recaudacionBruta} es la suma de los precios de lista, que es el valor
 * declarado de cada localidad; {@code descuentos} es lo que resignó el cine por una
 * promoción comercial suya; y {@code recaudacionNeta} lo que entró de verdad en la caja.
 * Con una sola no se puede explicar por qué la caja no da igual a entradas × precio.
 *
 * <p>Es el resultado de una cuenta del negocio y no una forma de mostrarla: por eso el
 * desglose lleva el enum {@link TipoTarifa} como clave y no su nombre en texto, igual que
 * el arqueo con los medios de pago.
 *
 * @param porTarifa el desglose que pide el organismo: cuántas entradas se vendieron a cada
 *                  precio. Es donde se ve qué proporción salió a tarifa reducida
 * @param generadoEn cuándo se emitió esta declaración. Lo sella el gestor, que es el que
 *                   conoce el reloj: el generador del archivo solo da formato
 */
public record Bordero(int funcionId, String pelicula, String sala, LocalDateTime funcion,
                      LocalDateTime generadoEn, int espectadores,
                      Dinero recaudacionBruta, Dinero descuentos, Dinero recaudacionNeta,
                      Map<TipoTarifa, TotalPorTarifa> porTarifa) {

    /** Cuántas entradas se vendieron a esa tarifa y cuánto sumaron a precio de lista. */
    public record TotalPorTarifa(int cantidad, Dinero total) {
    }
}
