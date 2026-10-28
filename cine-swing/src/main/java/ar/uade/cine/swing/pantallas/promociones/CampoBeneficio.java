package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Validacion;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.Optional;

// Cada campo de beneficio de una promoción: nombre en el JSON, etiqueta, si es entero y valor inicial.
// Qué campos pide cada tipo lo dice {@code GET /api/tipos-promocion}, no este enum.
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

    /** El valor que ya tiene la promoción que se edita, sin el ".0" de los enteros. */
    void poner(JTextField caja, Promocion p) {
        Number valor = switch (this) {
            case PORCENTAJE -> p.porcentaje();
            case MONTO -> p.monto();
            case LLEVA -> p.lleva();
            case PAGA -> p.paga();
        };
        if (valor == null) reiniciar(caja);
        else caja.setText(valor.doubleValue() % 1 == 0 ? String.valueOf(valor.longValue()) : String.valueOf(valor));
    }

    Number leer(Validacion v, JTextField caja) {
        // Sin ternario: con Integer y Double lo desempaqueta a double (NxM viajaba 3.0 y un vacío daba NPE).
        if (entero) return v.entero(caja, etiqueta, true);
        return v.decimal(caja, etiqueta, true);
    }
}
