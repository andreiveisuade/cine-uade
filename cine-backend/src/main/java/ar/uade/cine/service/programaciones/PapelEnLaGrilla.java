package ar.uade.cine.service.programaciones;

import lombok.Getter;

import ar.uade.cine.model.salas.TipoSala;

// Qué películas prefiere cada sala en la grilla, según los atributos de su tipo; la API lo muestra tal cual.
// Sale de TipoSala (premium, soportaTresD) y no del nombre de la constante: un tipo nuevo ya trae su papel.
@Getter
public enum PapelEnLaGrilla {
    MEJOR_PUNTUADAS("Premium: las mejor puntuadas, en 3D si tienen versión 3D"),
    VERSIONES_3D("3D: las versiones 3D; si no hay ninguna, en 2D"),
    EN_2D("2D: las películas en 2D, primero las que todavía no tienen pase ese día");

    private final String descripcion;

    PapelEnLaGrilla(String descripcion) {
        this.descripcion = descripcion;
    }

    public static PapelEnLaGrilla de(TipoSala tipo) {
        if (tipo.premium()) {
            return MEJOR_PUNTUADAS;
        }
        return tipo.soportaTresD() ? VERSIONES_3D : EN_2D;
    }
}
