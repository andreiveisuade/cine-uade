package ar.uade.cine.model.promociones.validacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.validacion.Regla;
import ar.uade.cine.model.ventas.validacion.ValidadorReserva;

// Los datos de una promoción: nombre, vigencia que alguna vez aplica y el beneficio de cada tipo.
// Cada guarda devuelve el valor ya limpio, así se valida todo antes de asignar.
public final class ValidadorPromocion {

    // El VARCHAR(60) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO_DEL_NOMBRE = 60;

    // El tope de butacas por compra: un NxM que pide llevar más no se completa nunca en una sola compra.
    private static final int LLEVA_MAXIMO = ValidadorReserva.MAXIMO_BUTACAS;

    private ValidadorPromocion() {
    }

    // Recortado acá: el nombre repetido se busca con el mismo valor que se guarda.
    public static String nombre(String nombre) {
        return Regla.texto(nombre).obligatorio("Falta el nombre").recortado()
                .hasta(LARGO_MAXIMO_DEL_NOMBRE, "El nombre").valor();
    }

    public static void exigirMismoTipo(TipoPromocion actual, TipoPromocion pedido) {
        if (actual != pedido) {
            throw new DatoInvalido("El tipo de una promoción no se puede cambiar");
        }
    }

    public static Periodo vigencia(LocalDate desde, LocalDate hasta) {
        Regla.objeto(desde).obligatorio("Falta el inicio de la vigencia");
        Regla.objeto(hasta).obligatorio("Falta el fin de la vigencia");
        return Periodo.de(desde, hasta, "La vigencia");
    }

    // Una que nunca va a correr no se guarda: vencida, o con días que no caen en lo que le queda de vigencia.
    public static void exigirQueAlgunaVezAplique(Periodo vigencia, Set<DayOfWeek> dias, LocalDate hoy) {
        if (vigencia.hasta().isBefore(hoy)) {
            throw new DatoInvalido("La vigencia ya terminó: el fin tiene que ser hoy o después");
        }
        LocalDate primerDia = vigencia.desde().isBefore(hoy) ? hoy : vigencia.desde();
        boolean algunoCae = dias.isEmpty() || primerDia.datesUntil(vigencia.hasta().plusDays(1))
                .anyMatch(dia -> dias.contains(dia.getDayOfWeek()));
        if (!algunoCae) {
            throw new DatoInvalido("Ninguno de los días elegidos cae en lo que queda de la vigencia:"
                    + " elegí otro día o extendé la vigencia");
        }
    }

    // Dos decimales: DECIMAL(5,2) redondearía 99,999 a 100 (entrada gratis).
    public static double porcentaje(Double porcentaje) {
        return Regla.numero(porcentaje).obligatorio("Falta el porcentaje")
                .entre(1.0, 99.0, "El porcentaje tiene que estar entre 1 y 99")
                .conDecimales(2, "El porcentaje tiene que tener como máximo 2 decimales")
                .valor();
    }

    public static Dinero monto(Double monto) {
        return Dinero.importe(monto, "monto del descuento");
    }

    // Un 2x2 no descuenta, un 2x3 cobraría de más y un 1x0 regalaría la entrada.
    public static void exigirNxM(Integer lleva, Integer paga) {
        Regla.numero(lleva).obligatorio("Falta cuántas entradas lleva");
        Regla.numero(paga).obligatorio("Falta cuántas entradas paga")
                .mayorQueCero("Un NxM tiene que cobrar al menos una entrada");
        if (lleva <= paga) {
            throw new DatoInvalido("En un NxM hay que llevar más de lo que se paga");
        }
        Regla.numero(lleva).entre(1, LLEVA_MAXIMO, "Un NxM tiene que llevar como máximo " + LLEVA_MAXIMO
                + " entradas, el tope de butacas por compra");
    }
}
