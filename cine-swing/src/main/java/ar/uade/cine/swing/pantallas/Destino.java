package ar.uade.cine.swing.pantallas;

/**
 * Las entradas del menú. Enum y no el título como texto: un "Ir a Salas" que apunta a un destino renombrado no
 * compila, en vez de no hacer nada sin avisar.
 */
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

    public String titulo() {
        return titulo;
    }
}
