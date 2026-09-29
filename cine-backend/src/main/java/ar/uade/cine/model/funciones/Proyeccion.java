package ar.uade.cine.model.funciones;

import ar.uade.cine.model.salas.TipoSala;

// Formato en que se proyecta una función, 2D o 3D; viaja por la API con el nombre de la constante.
public enum Proyeccion {
    DOS_D,
    TRES_D;

    // R8: el 3D necesita una sala que lo soporte; el 2D entra en cualquiera. Acá y no en la validación
    // porque también la pregunta la grilla automática, para repartir pases solo en las salas que sirven.
    public boolean sePuedeProyectarEn(TipoSala tipo) {
        return this != TRES_D || tipo.soportaTresD();
    }
}
