package ar.uade.cine.model.validacion;

import java.math.BigDecimal;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guardas de un número: obligatorio, positivo, en un rango o con N decimales; Fluent Interface desde Regla.
public final class ReglaDeNumero<N extends Number & Comparable<N>> {

    private final N valor;

    ReglaDeNumero(N valor) {
        this.valor = valor;
    }

    public ReglaDeNumero<N> obligatorio(String mensaje) {
        if (valor == null) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // En positivo a propósito: Jackson convierte "NaN" en Double.NaN, y `valor <= 0` lo dejaría pasar.
    public ReglaDeNumero<N> mayorQueCero(String mensaje) {
        if (valor != null && !(valor.doubleValue() > 0 && esFinito())) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public ReglaDeNumero<N> noNegativo(String mensaje) {
        if (valor != null && !(valor.doubleValue() >= 0 && esFinito())) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public ReglaDeNumero<N> entre(N minimo, N maximo, String mensaje) {
        if (valor != null && (!esFinito() || valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0)) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Para una columna DECIMAL: 99,999 % se guardaría redondeado a 100 %.
    public ReglaDeNumero<N> conDecimales(int maximo, String mensaje) {
        if (valor != null && (!esFinito() || decimales() > maximo)) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public N valor() {
        return valor;
    }

    private boolean esFinito() {
        return Double.isFinite(valor.doubleValue());
    }

    // Por BigDecimal: 0.1 en double no tiene un decimal, tiene cincuenta y cinco.
    private int decimales() {
        return Math.max(0, new BigDecimal(valor.toString()).stripTrailingZeros().scale());
    }
}
