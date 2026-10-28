package ar.uade.cine.swing.api;

// Cómo está un servicio de producción (Render, Aiven) según el panel de estado; `motivo` es opcional.
public record EstadoServicio(Fase fase, String motivo) {

    public enum Fase {
        APAGADO("Apagado"), ENCENDIENDO("Encendiendo"), EN_LINEA("En línea"), DESCONOCIDO("Desconocido"),
        ERROR("Error");

        private final String texto;

        Fase(String texto) {
            this.texto = texto;
        }
    }

    public static EstadoServicio de(Fase fase) {
        return new EstadoServicio(fase, null);
    }

    public static EstadoServicio error(String motivo) {
        return new EstadoServicio(Fase.ERROR, motivo);
    }

    public boolean enLinea() {
        return fase == Fase.EN_LINEA;
    }

    public String texto() {
        return motivo == null ? fase.texto : fase.texto + ": " + motivo;
    }
}
