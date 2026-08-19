package ar.uade.cine.model.dinero;

import java.util.Collection;

/**
 * Una cantidad de plata. Guarda <strong>centavos enteros</strong>, no pesos con coma.
 *
 * <h2>Por qué existe</h2>
 * <p>Antes toda la plata del sistema era {@code double}, y un {@code double} no puede
 * representar 0,10 exactamente: lo más cerca que llega es 0,1000000000000000055511151231…
 * Eso hace que las cuentas de dinero mientan de a poco. El caso que se veía en este
 * sistema estaba escrito en un comentario de la calculadora de precios:
 *
 * <pre>5250.50 × 1.3 = 6825.650000000001</pre>
 *
 * <p>La respuesta de entonces fue redondear a dos decimales después de cada operación. Eso
 * tapa el síntoma pero no la causa, y tiene dos problemas: hay que <em>acordarse</em> de
 * redondear en cada cuenta nueva —y el redondeo terminó copiado en dos clases de dos
 * paquetes distintos, {@code CalculadoraPrecio} y {@code PromocionPorcentaje}—, y el error
 * vuelve a acumularse en cuanto hay una suma larga sin redondeo intermedio, que es
 * exactamente lo que hace el arqueo del día al sumar trescientos cobros.
 *
 * <p>Con centavos enteros el problema no existe: no hay decimal que aproximar, la suma de
 * mil importes es exacta, y {@code equals} compara plata de verdad y no dos flotantes que
 * se parecen. El redondeo pasa <strong>una sola vez</strong>, cuando se entra desde un
 * número con decimales, y de ahí en adelante las cuentas son exactas.
 *
 * <h2>Dónde vive y dónde no</h2>
 * <p>Es el tipo de la plata en el dominio y en los servicios. En los <strong>bordes</strong>
 * vuelve a ser un número con decimales, en los dos únicos lugares donde hay que hablar el
 * idioma de otro:
 * <ul>
 *   <li>los DTO de {@code dto/}, porque el JSON del contrato dice {@code "total": 15000.0}
 *       y cambiarlo rompería el front sin ganar nada;
 *   <li>los DAO, porque la columna de la base es DECIMAL y el schema ya está desplegado.
 * </ul>
 * <p>La conversión se hace en el borde y nunca en el medio: {@link #aPesos()} al salir,
 * {@link #de(double)} al entrar.
 *
 * <p>No es {@code Comparable} por casualidad: comparar plata es una operación del negocio
 * —cuál promoción descuenta más (R15)— y necesita estar bien definida.
 *
 * @param centavos la cantidad, en centavos. Puede ser negativa: una diferencia de caja
 *                 hacia abajo es un número válido, y prohibirlo obligaría a que quien resta
 *                 sepa de antemano cuál de los dos es mayor
 */
public record Dinero(long centavos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);

    /** Cuántos centavos tiene un peso. Está acá y no suelto por ahí como un 100 mágico. */
    private static final int CENTAVOS_POR_PESO = 100;

    /**
     * Desde un importe en pesos con decimales. Es la única puerta por donde entra un
     * redondeo, y por eso es la única que puede perder un fracción de centavo: media
     * unidad se va para arriba, que es lo que espera cualquiera que mire un precio.
     */
    public static Dinero de(double pesos) {
        return new Dinero(Math.round(pesos * CENTAVOS_POR_PESO));
    }

    public static Dinero deCentavos(long centavos) {
        return new Dinero(centavos);
    }

    /**
     * El importe en pesos, para el JSON y para la base. Solo en los bordes: usarlo para
     * hacer una cuenta y volver a entrar devuelve el problema que esta clase resuelve.
     */
    public double aPesos() {
        return centavos / (double) CENTAVOS_POR_PESO;
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(centavos + otro.centavos);
    }

    public Dinero menos(Dinero otro) {
        return new Dinero(centavos - otro.centavos);
    }

    /**
     * Multiplicado por un factor sin unidad: los multiplicadores de sala, de butaca y de
     * tarifa. El resultado se redondea al centavo, porque medio centavo no se puede cobrar.
     *
     * <p>El factor sigue siendo {@code double} a propósito: 1,3 es una proporción, no una
     * cantidad de plata, y no tiene por qué ser exacta.
     */
    public Dinero por(double factor) {
        return new Dinero(Math.round(centavos * factor));
    }

    /** Un porcentaje de este importe: el "30% off" de una promoción. */
    public Dinero porcentaje(double porcentaje) {
        return por(porcentaje / 100.0);
    }

    /**
     * Este importe, pero nunca más que el techo. Es lo que impide que un descuento de
     * $2000 sobre una entrada de $1500 la deje en menos ochenta.
     */
    public Dinero acotadoA(Dinero techo) {
        return centavos > techo.centavos ? techo : this;
    }

    public Dinero sinBajarDeCero() {
        return centavos < 0 ? CERO : this;
    }

    public boolean esCero() {
        return centavos == 0;
    }

    public boolean esMayorQue(Dinero otro) {
        return centavos > otro.centavos;
    }

    /** La suma de varios importes. Exacta: no hay redondeo intermedio que acumular. */
    public static Dinero sumar(Collection<Dinero> importes) {
        long total = 0;
        for (Dinero importe : importes) {
            total += importe.centavos;
        }
        return new Dinero(total);
    }

    @Override
    public int compareTo(Dinero otro) {
        return Long.compare(centavos, otro.centavos);
    }

    /**
     * Con dos decimales y punto, como lo escriben los comprobantes y como lo espera el
     * JSON. Sin símbolo de moneda: ponerlo es decisión de quien muestra.
     */
    @Override
    public String toString() {
        long pesos = centavos / CENTAVOS_POR_PESO;
        long resto = Math.abs(centavos % CENTAVOS_POR_PESO);
        String signo = centavos < 0 && pesos == 0 ? "-" : "";
        return signo + pesos + "." + (resto < 10 ? "0" : "") + resto;
    }
}
