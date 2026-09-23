package ar.uade.cine.model.dinero;

import java.util.Collection;

/**
 * Una cantidad de plata, en <strong>centavos enteros</strong>.
 *
 * <p>Un {@code double} no representa 0,10 exacto y las cuentas mienten de a poco
 * ({@code 5250.50 × 1.3 = 6825.650000000001}); redondear después de cada operación tapa el
 * síntoma y hay que acordarse en cada cuenta nueva. Con centavos enteros la suma de mil
 * importes es exacta, {@code equals} compara plata de verdad y el redondeo pasa una sola
 * vez, al entrar desde un número con decimales.
 *
 * <p>Vuelve a ser decimal solo en los bordes: {@link #aPesos()} hacia el JSON y la
 * columna DECIMAL, {@link #de(double)} al entrar. Es {@code Comparable} porque comparar
 * plata es una operación del negocio (R15: gana la promoción que más descuenta).
 *
 * @param centavos puede ser negativo: una diferencia de caja hacia abajo es un número válido
 */
public record Dinero(long centavos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);

    private static final int CENTAVOS_POR_PESO = 100;

    /** Desde pesos con decimales. La única puerta por donde entra un redondeo. */
    public static Dinero de(double pesos) {
        return new Dinero(Math.round(pesos * CENTAVOS_POR_PESO));
    }

    public static Dinero deCentavos(long centavos) {
        return new Dinero(centavos);
    }

    /** El importe en pesos, solo para los bordes: hacer cuentas con esto devuelve el problema. */
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
     * Por un factor sin unidad (multiplicadores de sala, butaca, tarifa), redondeado al
     * centavo. El factor es {@code double} porque 1,3 es una proporción, no plata.
     */
    public Dinero por(double factor) {
        return new Dinero(Math.round(centavos * factor));
    }

    /** Un porcentaje de este importe: el "30% off" de una promoción. */
    public Dinero porcentaje(double porcentaje) {
        return por(porcentaje / 100.0);
    }

    /** Nunca más que el techo: un descuento de $2000 sobre $1500 no deja la entrada en negativo. */
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

    /** Con dos decimales y punto, sin símbolo de moneda: ponerlo es decisión de quien muestra. */
    @Override
    public String toString() {
        long pesos = centavos / CENTAVOS_POR_PESO;
        long resto = Math.abs(centavos % CENTAVOS_POR_PESO);
        String signo = centavos < 0 && pesos == 0 ? "-" : "";
        return signo + pesos + "." + (resto < 10 ? "0" : "") + resto;
    }
}
