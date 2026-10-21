package ar.uade.cine.swing.pantallas.acciones;

// La confirmación de una acción de fila: el texto y los dos botones, con el verbo de la acción en el que confirma.
public record Pregunta(String texto, String si, String no) {

    /** El de no es "Cancelar", salvo que la acción misma se llame así. */
    public Pregunta(String texto, String si) {
        this(texto, si, "Cancelar");
    }
}
