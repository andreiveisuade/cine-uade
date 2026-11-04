package ar.uade.cine.model.funciones;

import lombok.Getter;

import ar.uade.cine.model.dinero.Dinero;

// Formato de una función, 2D o 3D, con su recargo fijo; viaja por la API con el nombre de la constante.
@Getter
public enum Proyeccion {
    DOS_D("2D", Dinero.CERO),
    TRES_D("3D", Dinero.de(1000));

    private final String etiqueta;
    // Los anteojos: un monto fijo por entrada, que no multiplican ni la sala, ni la butaca, ni la tarifa.
    private final Dinero recargo;

    Proyeccion(String etiqueta, Dinero recargo) {
        this.etiqueta = etiqueta;
        this.recargo = recargo;
    }

    public Dinero conRecargo(Dinero precio) {
        return precio.mas(recargo);
    }
}
