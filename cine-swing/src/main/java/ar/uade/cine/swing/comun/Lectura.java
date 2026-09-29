package ar.uade.cine.swing.comun;

// El valor leído de un campo, o por qué no se pudo leer; con error, {@code valor} es null.
public record Lectura<T>(T valor, String error) {

    static <T> Lectura<T> de(T valor) {
        return new Lectura<>(valor, null);
    }

    static <T> Lectura<T> falla(String error) {
        return new Lectura<>(null, error);
    }

    public boolean valida() {
        return error == null;
    }
}
