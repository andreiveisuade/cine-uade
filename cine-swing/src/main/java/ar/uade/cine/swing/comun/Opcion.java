package ar.uade.cine.swing.comun;

import java.util.List;
import java.util.function.Function;

// Lo que va en un JComboBox: el valor que viaja al backend y el texto que ve el encargado.
public record Opcion<T>(T valor, String texto) {

    public static <T> List<Opcion<T>> de(List<T> valores, Function<T, String> texto) {
        return valores.stream().map(v -> new Opcion<>(v, texto.apply(v))).toList();
    }

    @Override
    public String toString() {
        return texto;
    }
}
