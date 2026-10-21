package ar.uade.cine.infrastructure.comprobantes.txt;

import java.util.Map;

// El comprobante lo lee una persona: nunca el nombre de la constante. Es el mismo diccionario que
// etiquetas.js en la web y Formato en Swing, del lado de quien presenta, no en el modelo.
final class EtiquetasTxt {

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("DOS_D", "2D"), Map.entry("TRES_D", "3D"), Map.entry("IMAX", "IMAX"),
            Map.entry("CUATRO_D", "4D"), Map.entry("DOBLADA", "doblada"), Map.entry("SUBTITULADA", "subtitulada"),
            Map.entry("RESERVADA", "Reservada"), Map.entry("PAGADA", "Pagada"), Map.entry("CANCELADA", "Cancelada"),
            Map.entry("EXPIRADA", "Vencida"), Map.entry("MENOR", "Menor"), Map.entry("JUBILADO", "Jubilado"),
            Map.entry("ESTUDIANTE", "Estudiante"), Map.entry("EFECTIVO", "Efectivo"), Map.entry("DEBITO", "Débito"),
            Map.entry("CREDITO", "Crédito"), Map.entry("QR", "QR"), Map.entry("TRANSFERENCIA", "Transferencia"));

    private EtiquetasTxt() {
    }

    static String de(Enum<?> valor) {
        return ETIQUETAS.getOrDefault(valor.name(), valor.name());
    }
}
