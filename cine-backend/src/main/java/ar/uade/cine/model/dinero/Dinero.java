package ar.uade.cine.model.dinero;

import java.util.Collection;

// Importe en pesos; Value Object inmutable con aritmética de precios y totales, y tope de lo que se carga.
// Centavos enteros y no double, que no representa 0,10 exacto.
public record Dinero(long centavos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);

    private static final int CENTAVOS_POR_PESO = 100;

    // Las columnas de plata son DECIMAL(10,2): pasado $ 99.999.999,99, MySQL rechaza el INSERT y el usuario
    // vería un 500. Un millón deja lugar para lo que se calcula encima: la entrada multiplica el precio por
    // sala, butaca y tarifa, y lo que más recarga hoy es 3,24.
    public static final Dinero IMPORTE_MAXIMO = new Dinero(1_000_000L * CENTAVOS_POR_PESO);

    public static Dinero de(double pesos) {
        return new Dinero(Math.round(pesos * CENTAVOS_POR_PESO));
    }

    public static Dinero deCentavos(long centavos) {
        return new Dinero(centavos);
    }

    // Un precio o monto que se carga a mano. Un solo lugar para producto, función, grilla y promoción: el
    // mismo problema dice el mismo texto en todas. «que» es el nombre del importe, masculino: "precio".
    public static Dinero importeValido(Dinero importe, String que) {
        if (importe == null) {
            throw new IllegalArgumentException("Falta el " + que);
        }
        if (!importe.esMayorQue(CERO)) {
            throw new IllegalArgumentException("El " + que + " tiene que ser mayor a cero");
        }
        if (importe.esMayorQue(IMPORTE_MAXIMO)) {
            throw new IllegalArgumentException("El " + que + " no puede superar $ " + IMPORTE_MAXIMO);
        }
        return importe;
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
