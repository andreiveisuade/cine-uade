package ar.uade.cine.model.cartelera;

import lombok.Getter;

import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.salas.TipoSala;

// Por qué se filtra la cartelera; Experto: cada tipo sabe qué función lo cumple, porque IMAX es una sala y no un formato.
@Getter
public enum TipoPelicula {
    DOS_D(Proyeccion.DOS_D, null),
    TRES_D(Proyeccion.TRES_D, null),
    IMAX(null, TipoSala.IMAX);

    private final Proyeccion proyeccion;
    private final TipoSala tipoSala;

    TipoPelicula(Proyeccion proyeccion, TipoSala tipoSala) {
        this.proyeccion = proyeccion;
        this.tipoSala = tipoSala;
    }
}
