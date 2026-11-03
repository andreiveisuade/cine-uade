package ar.uade.cine.service.programaciones;

import java.util.Collection;
import java.util.List;

// Menor tiempo muerto al cierre de una sala; Fabricación pura, programación dinámica (problema del cambio).
// "Pagar" los minutos que quedan con pases de limpieza + duración, quedando lo más cerca posible sin pasarse.
final class HuecoAlCierre {

    private final List<Integer> duraciones;
    private final int limpieza;
    // minimo[x]: el menor hueco final si una película termina x minutos antes del cierre.
    private final int[] minimo;

    HuecoAlCierre(Collection<Integer> duraciones, int limpieza, int minutosDelDia) {
        this.duraciones = List.copyOf(duraciones);
        this.limpieza = limpieza;
        this.minimo = new int[minutosDelDia + 1];
        for (int libres = 0; libres <= minutosDelDia; libres++) {
            minimo[libres] = libres;
            for (int duracion : this.duraciones) {
                int resto = libres - limpieza - duracion;
                if (resto >= 0) {
                    minimo[libres] = Math.min(minimo[libres], minimo[resto]);
                }
            }
        }
    }

    // El hueco que queda si ahora arranca un pase de esa duración y después va lo mejor posible.
    int siVa(int minutosHastaElCierre, int duracion) {
        return minimo[minutosHastaElCierre - duracion];
    }

    // Después de ese pase y su limpieza no entra ningún otro: es el último turno del día.
    boolean esElUltimo(int minutosHastaElCierre, int duracion) {
        int libres = minutosHastaElCierre - duracion - limpieza;
        return duraciones.stream().noneMatch(otra -> otra <= libres);
    }
}
