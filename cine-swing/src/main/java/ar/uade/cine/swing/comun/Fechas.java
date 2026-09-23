package ar.uade.cine.swing.comun;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JDayChooser;
import com.toedter.calendar.JTextFieldDateEditor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        JCalendar desplegable = new JCalendar(null, ARGENTINA, true, true);
        tematizar(desplegable);
        JDateChooser selector = new JDateChooser(desplegable, aDate(inicial), FORMATO_EN_PANTALLA,
                new EditorDeFecha());
        selector.setLocale(ARGENTINA);
        // El constructor no le pasa el formato al editor propio: sin esto se ve "23 sept 2026".
        selector.setDateFormatString(FORMATO_EN_PANTALLA);
        selector.setPreferredSize(new Dimension(140, selector.getPreferredSize().height));
        return selector;
    }

    public static JCalendar calendario(LocalDate inicial) {
        JCalendar calendario = new JCalendar(aDate(inicial), ARGENTINA, true, true);
        tematizar(calendario);
        return calendario;
    }

    /**
     * JCalendar trae colores fijos pensados para fondo blanco: encabezado celeste, domingos bordó, el día elegido
     * gris claro. Con el tema oscuro eso se lee mal, así que se pasan a los del tema. El día elegido no tiene setter
     * (es un campo protected que JDayChooser pinta en cada redibujo), por eso va por reflexión.
     */
    private static void tematizar(JCalendar calendario) {
        JDayChooser dias = calendario.getDayChooser();
        dias.setWeekOfYearVisible(false);
        dias.setDecorationBackgroundColor(UIManager.getColor("TableHeader.background"));
        dias.setDecorationBordersVisible(false);
        dias.setWeekdayForeground(Colores.secundario());
        dias.setSundayForeground(Colores.error());
        dias.setForeground(Colores.texto());
        try {
            Field elegido = JDayChooser.class.getDeclaredField("selectedColor");
            elegido.setAccessible(true);
            elegido.set(dias, Colores.seleccion());
        } catch (ReflectiveOperationException e) {
            // Sin acceso queda el gris de la librería: se ve peor, pero el calendario anda igual.
        }
        // Con el margen y el borde de FlatLaf, "lun." no entra en el botón del día de la semana y se ve "...".
        Component[] botones = dias.getDayPanel().getComponents();
        for (int i = 0; i < botones.length; i++) {
            if (!(botones[i] instanceof JButton boton)) continue;
            boton.setMargin(new Insets(0, 0, 0, 0));
            if (i < 7) boton.setBorder(BorderFactory.createEmptyBorder());
        }
        dias.setDay(dias.getDay());
        // El año es un JSpinField que se repinta en negro fijo al cambiar: se lo devuelve al color del tema.
        for (JTextField campo : camposDe(calendario.getYearChooser())) {
            campo.addPropertyChangeListener("foreground", e -> {
                Color tema = UIManager.getColor("TextField.foreground");
                if (!tema.equals(e.getNewValue()) && !Colores.error().equals(e.getNewValue())) {
                    campo.setForeground(Color.RED.equals(e.getNewValue()) ? Colores.error() : tema);
                }
            });
            campo.setForeground(UIManager.getColor("TextField.foreground"));
        }
    }

    private static List<JTextField> camposDe(Container contenedor) {
        List<JTextField> campos = new ArrayList<>();
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JTextField campo) campos.add(campo);
            if (hijo instanceof Container otro) campos.addAll(camposDe(otro));
        }
        return campos;
    }

    /**
     * El editor de texto de JDateChooser pinta la fecha en negro fijo (y en verde o rojo mientras se tipea): sobre el
     * tema oscuro el negro no se ve. Se traducen esos colores a los del tema.
     */
    private static final class EditorDeFecha extends JTextFieldDateEditor {

        @Override
        public void setForeground(Color color) {
            if (Color.RED.equals(color)) {
                super.setForeground(Colores.error());
            } else {
                super.setForeground(UIManager.getColor("TextField.foreground"));
            }
        }
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
