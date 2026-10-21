package ar.uade.cine.swing.pantallas;

import lombok.Getter;
import lombok.experimental.Accessors;

// Las entradas del menú del panel; enum y no texto, para que un «Ir a» a un destino renombrado no compile.
@Getter
@Accessors(fluent = true)
public enum Destino {

    PELICULAS("Películas"),
    POR_REVISAR("Por revisar"),
    IMPORTADOR("Importador"),
    SALAS("Salas"),
    FUNCIONES("Funciones"),
    GRILLA("Grilla"),
    PLANIFICADOR("Planificador"),
    AGENDA("Agenda"),
    RESERVAS("Reservas"),
    PROMOCIONES("Promociones"),
    CANDY("Candy"),
    CAJA("Caja"),
    DECLARACION_JURADA("Declaración jurada"),
    PUERTA("Puerta");

    private final String titulo;

    Destino(String titulo) {
        this.titulo = titulo;
    }
}
