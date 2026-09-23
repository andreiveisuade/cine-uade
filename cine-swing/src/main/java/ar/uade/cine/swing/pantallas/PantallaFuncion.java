package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.Bordero;
import ar.uade.cine.swing.api.dto.Funcion;
import ar.uade.cine.swing.api.dto.InformeFuncion;
import ar.uade.cine.swing.api.dto.Tarifa;
import ar.uade.cine.swing.api.dto.Total;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.informes.BorderoTxt;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

/** Borderó e informe de una función. Van juntos y no en pantallas propias: los dos se piden por funcionId. */
final class PantallaFuncion extends Pantalla {

    private record Datos(Funcion funcion, Bordero bordero, InformeFuncion informe) {
    }

    private record Emision(Bordero bordero, List<String> tarifas) {
    }

    private final int funcionId;
    private final JPanel cuerpo = new JPanel(new GridLayout(1, 2, 16, 0));
    private final JLabel subtitulo = new JLabel(" ");

    PantallaFuncion(ApiHttp api, Navegacion navegacion, int funcionId) {
        super(api, "Borderó e informe", null);
        this.funcionId = funcionId;

        JButton volver = new JButton("← Funciones");
        volver.addActionListener(e -> navegacion.ir("Funciones"));
        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.add(volver, BorderLayout.WEST);
        norte.add(subtitulo, BorderLayout.SOUTH);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(norte, BorderLayout.NORTH);
        arriba.add(cuerpo, BorderLayout.CENTER);
        add(arriba, BorderLayout.CENTER);

        cargar(() -> new Datos(api.obtenerFuncion(funcionId), api.obtenerBordero(funcionId),
                api.obtenerInformeDeFuncion(funcionId)), this::pintar);
    }

    private void pintar(Datos datos) {
        Funcion f = datos.funcion();
        subtitulo.setText("<html><span style='font-size:18pt'><b>" + f.pelicula().titulo() + "</b></span><br>"
                + dia(f.inicio()) + " " + hora(f.inicio()) + " · " + f.sala().nombre() + " ("
                + etiqueta(f.sala().tipo()) + ") · " + etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())
                + " · precio base " + precio(f.precio()) + " · " + (f.libres() == null ? "?" : f.libres())
                + " de " + f.sala().capacidadSala() + " butacas libres</html>");
        cuerpo.removeAll();
        cuerpo.add(new PanelBordero(datos.bordero()));
        cuerpo.add(panelInforme(datos.informe()));
        cuerpo.revalidate();
        cuerpo.repaint();
    }

    /** `emitido`: consultar el borderó y declararlo no son lo mismo, y no se muestran igual. */
    private final class PanelBordero extends JPanel {

        private final JPanel contenido = new JPanel();

        PanelBordero(Bordero bordero) {
            super(new BorderLayout());
            add(Componentes.conBorde(contenido));
            pintar(bordero, false);
        }

        private void pintar(Bordero bordero, boolean emitido) {
            contenido.removeAll();
            contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
            agregar(contenido, Componentes.subtitulo("Borderó"));
            agregar(contenido, Componentes.nota("Lo que se declara al INCAA. Cuenta lo <b>cobrado</b>: una reserva "
                    + "sin pagar retiene butacas pero no vendió ninguna entrada."));
            contenido.add(Box.createVerticalStrut(8));

            List<Map.Entry<String, Total>> tarifas = List.copyOf(bordero.porTarifa().entrySet());
            if (tarifas.isEmpty()) {
                agregar(contenido, Componentes.nota("Todavía no se cobró ninguna entrada de esta función. No es un "
                        + "error: es un borderó en cero, y se puede declarar igual."));
            } else {
                Tabla<Map.Entry<String, Total>> tabla = new Tabla<>(
                        Columna.<Map.Entry<String, Total>>de("Tarifa", e -> etiqueta(e.getKey())),
                        Columna.<Map.Entry<String, Total>>numero("Entradas", e -> e.getValue().cantidad()),
                        Columna.<Map.Entry<String, Total>>numero("Total", e -> precio(e.getValue().total())));
                tabla.mostrar(tarifas);
                var scroll = tabla.conScroll();
                scroll.setPreferredSize(new Dimension(300, Tabla.altoPara(tarifas.size())));
                scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tabla.altoPara(tarifas.size())));
                agregar(contenido, scroll);
                agregar(contenido, new JLabel(bordero.espectadores() + " espectadores"));
            }
            contenido.add(Box.createVerticalStrut(8));
            agregar(contenido, renglon("Recaudación bruta", precio(bordero.recaudacionBruta()), false));
            agregar(contenido, renglon("Descuentos", "− " + precio(bordero.descuentos()), false));
            agregar(contenido, new JSeparator());
            agregar(contenido, renglon("Recaudación neta", precio(bordero.recaudacionNeta()), true));
            agregar(contenido, Componentes.nota("Las tres van separadas porque cuentan cosas distintas: la bruta es a "
                    + "precio de lista, los descuentos son lo que resignó el cine por una promoción suya, y la neta "
                    + "es lo que entró en la caja."));
            contenido.add(Box.createVerticalStrut(12));

            JButton emitir = new JButton("Emitir borderó");
            emitir.addActionListener(e -> emitir());
            JPanel fila = new JPanel(new BorderLayout(8, 0));
            fila.add(emitir, BorderLayout.WEST);
            fila.add(Componentes.nota(emitido
                    ? "Emitido el " + fechaHora(bordero.generadoEn()) + "."
                    : "Consultado el " + fechaHora(bordero.generadoEn()) + "."), BorderLayout.CENTER);
            // BoxLayout reparte el sobrante entre lo que no tiene alto máximo: sin tope, el botón crecía.
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
            agregar(contenido, fila);
            agregar(contenido, Componentes.nota("Emitir vuelve a pedir el borderó y lo guarda como texto en esta "
                    + "PC. El de una función es uno solo y vale el último, porque las entradas se siguen vendiendo "
                    + "hasta que la película arranca."));
            contenido.add(Box.createVerticalGlue());
            contenido.revalidate();
            contenido.repaint();
        }

        // Se pide de nuevo al emitir: lo que está en pantalla puede haber quedado viejo.
        private void emitir() {
            cargar(() -> new Emision(api.obtenerBordero(funcionId),
                    api.obtenerTarifas().stream().map(Tarifa::nombre).toList()), emision -> {
                pintar(emision.bordero(), true);
                JFileChooser elegir = new JFileChooser();
                elegir.setSelectedFile(new File(BorderoTxt.nombreArchivo(funcionId)));
                if (elegir.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
                Path destino = elegir.getSelectedFile().toPath();
                try {
                    Files.writeString(destino, BorderoTxt.escribir(emision.bordero(), emision.tarifas()),
                            StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    Tarea.mostrarError(this, new ErrorApi(0, "No se pudo guardar el borderó: " + ex.getMessage()));
                    return;
                }
                avisar("Borderó emitido: " + emision.bordero().espectadores() + " espectadores, "
                        + precio(emision.bordero().recaudacionNeta()) + ".\nGuardado en " + destino);
            });
        }
    }

    private JPanel panelInforme(InformeFuncion informe) {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        agregar(contenido, Componentes.subtitulo("Informe de la función"));
        agregar(contenido, Componentes.nota("Cuánto dejó la función entre las dos cajas del cine: boletería y candy."));
        contenido.add(Box.createVerticalStrut(8));
        agregar(contenido, renglon("Boletería", precio(informe.boleteria().recaudacionNeta()), false));
        agregar(contenido, Componentes.nota(informe.boleteria().espectadores()
                + " entradas cobradas, ya con los descuentos"));
        agregar(contenido, renglon("Candy", precio(informe.candy()), false));
        agregar(contenido, Componentes.nota(informe.comprasCandy() + (informe.comprasCandy() == 1
                ? " compra atribuida" : " compras atribuidas") + " a esta función"));
        agregar(contenido, new JSeparator());
        agregar(contenido, renglon("Total", precio(informe.total()), true));
        contenido.add(Box.createVerticalStrut(12));
        agregar(contenido, Componentes.nota("<b>El candy de mostrador no entra, a propósito.</b> Solo se le atribuye "
                + "a una función lo que se compró junto con la entrada, porque esa reserva es lo único que dice de qué "
                + "función se trata. Por eso la suma de los informes de todas las funciones de un día da menos que el "
                + "arqueo de ese día, y la diferencia es el mostrador."));
        contenido.add(Box.createVerticalGlue());
        return Componentes.conBorde(contenido);
    }

    private static JPanel renglon(String texto, String valor, boolean fuerte) {
        JPanel fila = new JPanel(new BorderLayout());
        JLabel izquierda = new JLabel(texto);
        JLabel derecha = new JLabel(valor);
        if (fuerte) {
            izquierda.setFont(izquierda.getFont().deriveFont(Font.BOLD, 16f));
            derecha.setFont(derecha.getFont().deriveFont(Font.BOLD, 16f));
        }
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(derecha, BorderLayout.EAST);
        fila.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
        return fila;
    }

    private static void agregar(JPanel panel, JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(componente);
    }
}
