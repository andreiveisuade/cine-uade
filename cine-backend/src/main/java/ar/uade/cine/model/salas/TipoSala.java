package ar.uade.cine.model.salas;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;

// Formato de sala; Experto: trae su multiplicador de precio, qué proyecciones admite (R8, R35) y si es premium.
@Getter
public enum TipoSala {

    DOS_D(1.0, false, Proyeccion.DOS_D),
    TRES_D(1.3, false, Proyeccion.DOS_D, Proyeccion.TRES_D),
    IMAX(1.6, true, Proyeccion.DOS_D, Proyeccion.TRES_D),
    // R35: siempre en 3D, así que tampoco da una película que no se estrena en 3D (R31).
    CUATRO_D(1.8, true, Proyeccion.TRES_D);

    private final double multiplicadorPrecio;
    // La grilla le da las mejor puntuadas: es la sala que más cobra por la misma película.
    @Accessors(fluent = true)
    private final boolean premium;
    private final List<Proyeccion> proyecciones;

    TipoSala(double multiplicadorPrecio, boolean premium, Proyeccion... proyecciones) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.premium = premium;
        this.proyecciones = List.of(proyecciones);
    }

    public boolean admite(Proyeccion proyeccion) {
        return proyecciones.contains(proyeccion);
    }

    // Su parte del precio de una entrada: el recargo del formato sobre el precio base de la función.
    public Dinero aplicarA(Dinero precio) {
        return precio.por(multiplicadorPrecio);
    }
}
