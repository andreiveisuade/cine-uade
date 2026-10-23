package ar.uade.cine.swing.comun;

// El valor leído de un campo, o por qué no se pudo leer; con error, {@code valor} es null.
public record ValorLeido<T>(T valor, String error) {

    static <T> ValorLeido<T> de(T valor) {
        return new ValorLeido<>(valor, null);
    }

    static <T> ValorLeido<T> falla(String error) {
        return new ValorLeido<>(null, error);
    }

    public boolean valida() {
        return error == null;
    }
}
