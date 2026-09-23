package ar.uade.cine.model.dinero;

import java.util.Collection;

// Centavos enteros y no double, que no representa 0,10 exacto.
public record Dinero(long centavos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);

    private static final int CENTAVOS_POR_PESO = 100;

    public static Dinero de(double pesos) {
        return new Dinero(Math.round(pesos * CENTAVOS_POR_PESO));
    }

    public static Dinero deCentavos(long centavos) {
        return new Dinero(centavos);
    }

    public double aPesos() {
        return centavos / (double) CENTAVOS_POR_PESO;
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(centavos + otro.centavos);
    }

    public Dinero menos(Dinero otro) {
        return new Dinero(centavos - otro.centavos);
    }

    public Dinero por(double factor) {
        return new Dinero(Math.round(centavos * factor));
    }

    public Dinero porcentaje(double porcentaje) {
        return por(porcentaje / 100.0);
    }

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

    @Override
    public String toString() {
        long pesos = centavos / CENTAVOS_POR_PESO;
        long resto = Math.abs(centavos % CENTAVOS_POR_PESO);
        String signo = centavos < 0 && pesos == 0 ? "-" : "";
        return signo + pesos + "." + (resto < 10 ? "0" : "") + resto;
    }
}
