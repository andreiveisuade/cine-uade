package ar.uade.cine.swing.comun;

import java.util.Map;

// Copia de etiquetas.js del front: el backend manda el nombre de la constante y cada cliente lo traduce.
public final class Etiquetas {

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("ACCION", "Acción"),
            Map.entry("COMEDIA", "Comedia"),
            Map.entry("DRAMA", "Drama"),
            Map.entry("TERROR", "Terror"),
            Map.entry("CIENCIA_FICCION", "Ciencia ficción"),
            Map.entry("ANIMACION", "Animación"),
            Map.entry("DOCUMENTAL", "Documental"),
            Map.entry("ROMANCE", "Romance"),
            Map.entry("SUSPENSO", "Suspenso"),
            Map.entry("DOS_D", "2D"),
            Map.entry("TRES_D", "3D"),
            Map.entry("IMAX", "IMAX"),
            Map.entry("CUATRO_D", "4D"),
            Map.entry("VIP", "VIP"),
            Map.entry("ESTANDAR", "Estándar"),
            Map.entry("PAREJA", "Pareja"),
            Map.entry("ACCESIBLE", "Accesible"),
            Map.entry("DOBLADA", "Doblada"),
            Map.entry("SUBTITULADA", "Subtitulada"),
            Map.entry("HABILITADO", "Habilitada"),
            Map.entry("FUERA_DE_SERVICIO", "Fuera de servicio"),
            Map.entry("RESERVADA", "Reservada"),
            Map.entry("PAGADA", "Pagada"),
            Map.entry("CANCELADA", "Cancelada"),
            Map.entry("EXPIRADA", "Vencida"),
            Map.entry("GENERAL", "General"),
            Map.entry("MENOR", "Menor"),
            Map.entry("JUBILADO", "Jubilado"),
            Map.entry("ESTUDIANTE", "Estudiante"),
            Map.entry("PORCENTAJE", "Porcentaje"),
            Map.entry("MONTO_FIJO", "Monto fijo"),
            Map.entry("NXM", "NxM"),
            Map.entry("ADMINISTRADOR", "Administrador"),
            Map.entry("ACOMODADOR", "Acomodador"),
            Map.entry("MONDAY", "Lunes"),
            Map.entry("TUESDAY", "Martes"),
            Map.entry("WEDNESDAY", "Miércoles"),
            Map.entry("THURSDAY", "Jueves"),
            Map.entry("FRIDAY", "Viernes"),
            Map.entry("SATURDAY", "Sábado"),
            Map.entry("SUNDAY", "Domingo"),
            Map.entry("ATP", "ATP"),
            Map.entry("MAS_13", "+13"),
            Map.entry("MAS_16", "+16"),
            Map.entry("MAS_18", "+18"),
            Map.entry("EN_CURSO", "En curso"),
            Map.entry("TERMINADA", "Terminada"),
            Map.entry("FALLIDA", "Falló"),
            Map.entry("POCHOCLOS", "Pochoclos"),
            Map.entry("BEBIDA", "Bebida"),
            Map.entry("GOLOSINA", "Golosina"),
            Map.entry("COMBO", "Combo"),
            Map.entry("EFECTIVO", "Efectivo"),
            Map.entry("DEBITO", "Débito"),
            Map.entry("CREDITO", "Crédito"),
            Map.entry("QR", "QR"),
            Map.entry("TRANSFERENCIA", "Transferencia"));

    private Etiquetas() {
    }

    public static String etiqueta(String valor) {
        if (valor == null) return "";
        return ETIQUETAS.getOrDefault(valor, valor);
    }
}
