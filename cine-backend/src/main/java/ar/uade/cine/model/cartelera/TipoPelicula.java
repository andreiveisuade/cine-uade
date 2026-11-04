package ar.uade.cine.model.cartelera;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.salas.TipoSala;

// Por qué se filtra; Experto: cada tipo sabe qué función lo cumple: 2D y 3D por la proyección, IMAX y 4D por la sala.
@Getter
public enum TipoPelicula {
    DOS_D(Proyeccion.DOS_D, null),
    TRES_D(Proyeccion.TRES_D, null),
    IMAX(null, TipoSala.IMAX),
    CUATRO_D(null, TipoSala.CUATRO_D);

    private final Proyeccion proyeccion;
    private final TipoSala tipoSala;

    TipoPelicula(Proyeccion proyeccion, TipoSala tipoSala) {
        this.proyeccion = proyeccion;
        this.tipoSala = tipoSala;
    }

    // Una función entra en todos los que cumple: la 3D de una sala 4D está en los dos.
    public static List<TipoPelicula> de(Proyeccion proyeccion, TipoSala tipoSala) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.proyeccion == proyeccion || tipo.tipoSala == tipoSala)
                .toList();
    }
}
