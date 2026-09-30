package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Validacion;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.Optional;

// Cada campo de beneficio de una promoción: nombre en el JSON, etiqueta, si es entero y valor inicial.
/**
 * Cuáles pide cada tipo de promoción lo dice {@code GET /api/tipos-promocion}, no este enum: un tipo nuevo que use
 * estos campos no toca la pantalla.
 */
enum CampoBeneficio {

    PORCENTAJE("porcentaje", "Porcentaje", false, "30"),
    MONTO("monto", "Monto a descontar", false, "2000"),
    LLEVA("lleva", "Lleva", true, "2"),
    PAGA("paga", "Paga", true, "1");

    private final String nombre;
    private final String etiqueta;
    private final boolean entero;
    private final String inicial;

    CampoBeneficio(String nombre, String etiqueta, boolean entero, String inicial) {
        this.nombre = nombre;
        this.etiqueta = etiqueta;
        this.entero = entero;
        this.inicial = inicial;
    }

    /** El campo con ese nombre del JSON; uno que Swing no conoce se saltea. */
    static Optional<CampoBeneficio> llamado(String nombre) {
        return Arrays.stream(values()).filter(c -> c.nombre.equals(nombre)).findFirst();
    }

    String nombre() {
        return nombre;
    }

    String etiqueta() {
        return etiqueta;
    }

    /** La caja de texto del campo, con su valor inicial y sin dejar tipear lo que no es número. */
    JTextField caja() {
        JTextField caja = new JTextField(inicial);
        return entero ? Campos.soloEntero(caja) : Campos.soloDecimal(caja);
    }

    void reiniciar(JTextField caja) {
        caja.setText(inicial);
    }

    Number leer(Validacion v, JTextField caja) {
        return entero ? v.entero(caja, etiqueta, true) : v.decimal(caja, etiqueta, true);
    }
}
