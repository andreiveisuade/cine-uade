package ar.uade.cine.swing.comun;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * El único lugar donde se cruza {@link Date} (lo que usan JCalendar y los spinners) con {@code java.time}: en pantalla
 * la fecha se ve dd/MM/yyyy, y al backend le viaja ISO (2026-08-13) como pide el contrato.
 */
public final class Fechas {

    public static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final String FORMATO_EN_PANTALLA = "dd/MM/yyyy";
    private static final DateTimeFormatter ISO_COMPLETO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Fechas() {
    }

    /** {@code inicial} null deja el campo vacío, que en un filtro significa "sin filtro". */
    public static JDateChooser selector(LocalDate inicial) {
        JDateChooser selector = new JDateChooser();
        selector.setLocale(ARGENTINA);
        selector.setDateFormatString(FORMATO_EN_PANTALLA);
        selector.setDate(aDate(inicial));
        selector.setPreferredSize(new Dimension(140, selector.getPreferredSize().height));
        return selector;
    }

    public static JCalendar calendario(LocalDate inicial) {
        JCalendar calendario = new JCalendar(aDate(inicial), ARGENTINA, true, true);
        calendario.getDayChooser().setWeekOfYearVisible(false);
        return calendario;
    }

    /** Hora sola, HH:mm. El día se elige aparte con un selector: juntos arman el inicio de una función. */
    public static JSpinner hora(LocalTime inicial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "HH:mm"));
        spinner.setValue(aDate(LocalDate.now().atTime(inicial)));
        return spinner;
    }

    public static LocalDate leer(JDateChooser selector) {
        return aLocalDate(selector.getDate());
    }

    public static LocalTime leerHora(JSpinner spinner) {
        return aLocalDateTime((Date) spinner.getValue()).toLocalTime().withSecond(0).withNano(0);
    }

    /** Lo que viaja al backend: ISO, o null si el campo quedó vacío. */
    public static String iso(JDateChooser selector) {
        LocalDate fecha = leer(selector);
        return fecha == null ? null : fecha.toString();
    }

    /** Con segundos siempre: ISO_LOCAL_DATE_TIME los omite si son cero, y el backend espera 2026-08-13T20:30:00. */
    public static String isoCompleto(LocalDateTime momento) {
        return momento.format(ISO_COMPLETO);
    }

    public static void poner(JDateChooser selector, LocalDate fecha) {
        selector.setDate(aDate(fecha));
    }

    public static void poner(JCalendar calendario, LocalDate fecha) {
        calendario.setDate(aDate(fecha));
    }

    public static LocalDate leer(JCalendar calendario) {
        return aLocalDate(calendario.getDate());
    }

    public static Date aDate(LocalDate fecha) {
        return fecha == null ? null : Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date aDate(LocalDateTime momento) {
        return momento == null ? null : Date.from(momento.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate aLocalDate(Date fecha) {
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime aLocalDateTime(Date fecha) {
        return fecha == null ? null : LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault());
    }
}
