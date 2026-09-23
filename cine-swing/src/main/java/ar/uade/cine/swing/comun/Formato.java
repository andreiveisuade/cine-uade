package ar.uade.cine.swing.comun;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

// Los mismos formatos que cine-frontend/src/api/formato.js, para que la web del cliente y el escritorio digan lo mismo.
public final class Formato {

    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct",
            "nov", "dic"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String precio(double monto) {
        NumberFormat numero = NumberFormat.getIntegerInstance(ARGENTINA);
        return "$ " + numero.format(Math.round(monto));
    }

    public static String duracion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        return horas > 0 ? horas + "h " + resto + "m" : resto + "m";
    }

    public static String hora(String iso) {
        if (iso == null) return "—";
        return LocalDateTime.parse(iso).format(HORA);
    }

    public static String fechaHora(String iso) {
        if (iso == null) return "—";
        return LocalDateTime.parse(iso).format(FECHA_HORA);
    }

    public static String dia(String iso) {
        LocalDate fecha = LocalDateTime.parse(iso).toLocalDate();
        long diferencia = ChronoUnit.DAYS.between(LocalDate.now(), fecha);
        if (diferencia == 0) return "Hoy";
        if (diferencia == 1) return "Mañana";
        return DIAS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.getDayOfMonth() + " "
                + MESES[fecha.getMonthValue() - 1];
    }

    public static String hoyIso() {
        return LocalDate.now().toString();
    }
}
