package ar.uade.cine.swing.comun;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;

// Piezas de pantalla que se repiten, para que cada pantalla arme lo suyo y todas se vean igual.
// Todo espacio y todo tamaño de letra sale de las escalas de acá: sin números sueltos, las pantallas respiran igual.
public final class Componentes {

    // La escala de espacios, base 4: del más pegado al margen de la pantalla.
    public static final int PEGADO = 4;
    public static final int JUNTO = 8;
    public static final int CERCA = 12;
    public static final int APARTE = 16;
    public static final int BLOQUE = 24;
    public static final int MARGEN = 32;

    // La escala de letra: título de pantalla, de tarjeta, cuerpo es la del tema, nota y rótulo.
    public static final float LETRA_TITULO = 22f;
    public static final float LETRA_TARJETA = 16f;
    public static final float LETRA_NOTA = 12f;
    public static final float LETRA_ROTULO = 11f;

    private static final int ARCO = 12;

    private Componentes() {
    }

    /** Título de 22 y una bajada gris: lo primero que se lee de cada pantalla. */
    public static JPanel encabezado(String titulo, String descripcion) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, LETRA_TITULO));
        panel.add(izquierda(etiqueta));
        if (descripcion != null) {
            panel.add(Box.createVerticalStrut(PEGADO));
            JLabel bajada = nota(descripcion);
            // Una bajada de lectura, no un renglón de 1000 px: corta al ancho que se lee de un tirón.
            bajada.setMaximumSize(new Dimension(640, Integer.MAX_VALUE));
            panel.add(izquierda(bajada));
        }
        return panel;
    }

    /** Ancho del formulario al costado de una tabla, igual en todas las pantallas. */
    public static final int ANCHO_LATERAL = 340;

    /** Formulario junto a la tabla: ancho fijo, scroll solo vertical y del alto de lo que tiene, no de la ventana. */
    public static JScrollPane lateral(JComponent contenido) {
        AlAnchoDelVisor visor = new AlAnchoDelVisor(new BorderLayout(), true);
        visor.add(contenido, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(visor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(ANCHO_LATERAL, 0));
        return scroll;
    }

    /** Botones del mismo ancho, uno al lado del otro, como Guardar y Cancelar al pie de un formulario. */
    public static JPanel botones(JButton... botones) {
        JPanel fila = new JPanel(new GridLayout(1, botones.length, JUNTO, 0));
        for (JButton boton : botones) fila.add(boton);
        return fila;
    }

    /**
     * Botones a la derecha, del ancho de su texto y al ras del borde: las acciones de una tabla o el pie de un
     * formulario. El primario va último, así el paso que avanza queda siempre en el mismo lugar.
     */
    public static JPanel aLaDerecha(JComponent... componentes) {
        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.add(Box.createHorizontalGlue());
        for (JComponent componente : componentes) agregarALaDerecha(fila, componente);
        return fila;
    }

    /** Suma un botón más a una fila de {@link #aLaDerecha}, con el mismo espacio entre botones. */
    public static void agregarALaDerecha(JPanel fila, JComponent componente) {
        if (fila.getComponentCount() > 1) fila.add(Box.createHorizontalStrut(JUNTO));
        fila.add(componente);
    }

    /** El paso que avanza: ámbar, uno por pantalla. El estilo vive en el tema, {@code [style]Button.primario}. */
    public static <B extends JButton> B primario(B boton) {
        boton.putClientProperty(FlatClientProperties.STYLE_CLASS, "primario");
        return boton;
    }

    /** Lo que borra o da de baja: texto rojo, y siempre con confirmación. */
    public static <B extends JButton> B peligro(B boton) {
        boton.putClientProperty(FlatClientProperties.STYLE_CLASS, "peligro");
        return boton;
    }

    /** Volver, limpiar: sin borde, para que no compita con las acciones. */
    public static <B extends JButton> B deTexto(B boton) {
        boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        return boton;
    }

    /**
     * El listado de siempre: los filtros, después [resumen | acciones de la fila elegida] y la tabla. Las acciones
     * quedan pegadas a la tabla y a la vista del conteo: se elige y se actúa en el mismo lugar. Sin filtros, null.
     */
    public static JPanel listado(JComponent filtros, JComponent resumen, JComponent acciones, JComponent tabla) {
        JPanel barra = new JPanel(new BorderLayout(BLOQUE, 0));
        barra.add(resumen, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);
        JPanel arriba = new JPanel(new BorderLayout(0, CERCA));
        if (filtros != null) arriba.add(filtros, BorderLayout.NORTH);
        arriba.add(barra, BorderLayout.SOUTH);
        JPanel listado = new JPanel(new BorderLayout(0, JUNTO));
        listado.add(arriba, BorderLayout.NORTH);
        listado.add(tabla, BorderLayout.CENTER);
        return listado;
    }

    /** Cambia lo que muestra un panel: saca todo, pone lo nuevo y lo vuelve a dibujar. */
    public static void reemplazar(Container panel, Component... nuevos) {
        panel.removeAll();
        for (Component nuevo : nuevos) panel.add(nuevo);
        panel.revalidate();
        panel.repaint();
    }

    /** Texto largo que solo se lee, cortado por palabras: un informe, un ticket, un detalle. */
    public static JTextArea areaDeLectura(int filas, int columnas) {
        JTextArea area = new JTextArea(filas, columnas);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public static JLabel subtitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, LETRA_TARJETA));
        return etiqueta;
    }

    /** Texto gris que explica. Corta línea al ancho que le toque, en vez de estirar la ventana o cortarse. */
    public static JLabel nota(String texto) {
        JLabel etiqueta = texto(texto);
        etiqueta.setForeground(Colores.secundario());
        etiqueta.setFont(etiqueta.getFont().deriveFont(LETRA_NOTA));
        return etiqueta;
    }

    /** HTML que corta línea al ancho que le toque, con el color y la letra de siempre. */
    public static JLabel texto(String html) {
        return new TextoQueSalta("<html>" + html + "</html>");
    }

    public static JComponent izquierda(JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        return componente;
    }

    /** Una tarjeta: borde redondeado y aire adentro. Formularios, cifras, informes. */
    public static JPanel conBorde(JComponent contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        tarjeta(panel);
        panel.add(contenido);
        return panel;
    }

    /** Lo mismo para un panel que ya tiene su layout. */
    public static <P extends JComponent> P tarjeta(P panel) {
        panel.setBorder(new FlatLineBorder(new Insets(APARTE, APARTE, APARTE, APARTE), Colores.borde(), 1, ARCO));
        return panel;
    }

    /** Cifra grande con título arriba (arqueo, indicadores); {@code detalle} y {@code variacion} opcionales. */
    public static JPanel cifra(String titulo, String valor, String detalle, String variacion) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel arriba = new JLabel(titulo.toUpperCase());
        arriba.setForeground(Colores.secundario());
        arriba.setFont(arriba.getFont().deriveFont(LETRA_ROTULO));
        JLabel numero = new JLabel(valor);
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, LETRA_TITULO));
        panel.add(izquierda(arriba));
        panel.add(Box.createVerticalStrut(PEGADO));
        panel.add(izquierda(numero));
        if (detalle != null) panel.add(izquierda(texto(detalle)));
        if (variacion != null) {
            JLabel cambio = new JLabel(variacion);
            cambio.setFont(cambio.getFont().deriveFont(LETRA_ROTULO));
            panel.add(izquierda(cambio));
        }
        return conBorde(panel);
    }

    /** Un importe en un renglón: el texto a la izquierda y el valor a la derecha; {@code fuerte}, para el total. */
    public static JPanel renglon(String texto, String valor, boolean fuerte) {
        JPanel fila = new JPanel(new BorderLayout());
        JLabel izquierda = new JLabel(texto);
        JLabel derecha = new JLabel(valor);
        if (fuerte) {
            izquierda.setFont(izquierda.getFont().deriveFont(Font.BOLD, LETRA_TARJETA));
            derecha.setFont(derecha.getFont().deriveFont(Font.BOLD, LETRA_TARJETA));
        }
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(derecha, BorderLayout.EAST);
        fila.setBorder(BorderFactory.createEmptyBorder(PEGADO, 0, PEGADO, 0));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
        return fila;
    }
}
