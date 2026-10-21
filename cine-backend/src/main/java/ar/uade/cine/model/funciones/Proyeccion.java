package ar.uade.cine.model.funciones;

import ar.uade.cine.model.salas.TipoSala;

// Formato en que se proyecta una función, 2D o 3D; viaja por la API con el nombre de la constante.
public enum Proyeccion {
    DOS_D,
    TRES_D;

    // R8: el 3D necesita una sala que lo soporte. Acá porque también lo pregunta la grilla automática.
    public boolean sePuedeProyectarEn(TipoSala tipo) {
        return this != TRES_D || tipo.soportaTresD();
    }
}
