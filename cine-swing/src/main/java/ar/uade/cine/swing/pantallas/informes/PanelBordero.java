package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiInformes;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.catalogos.Tarifa;
import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.informes.Total;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.informes.BorderoTxt;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.precio;

// El borderó de una función y su emisión: se vuelve a pedir y se guarda como texto en esta PC.
/** {@code emitido}: consultar el borderó y declararlo no son lo mismo, y no se muestran igual. */
final class PanelBordero extends Seccion {

    private record Emision(Bordero bordero, List<String> tarifas) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiInformes apiInformes;
    private final int funcionId;
    private final JPanel contenido = new JPanel();

    PanelBordero(ApiCatalogos apiCatalogos, ApiInformes apiInformes, int funcionId, Bordero bordero) {
        super(new BorderLayout());
        this.apiCatalogos = apiCatalogos;
        this.apiInformes = apiInformes;
        this.funcionId = funcionId;
        add(Componentes.conBorde(contenido));
        pintar(bordero, false);
    }

    private void pintar(Bordero bordero, boolean emitido) {
        contenido.removeAll();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.add(Componentes.izquierda(Componentes.subtitulo("Borderó")));
        contenido.add(Componentes.izquierda(Componentes.nota("Lo que se declara al INCAA. Cuenta lo <b>cobrado</b>: "
                + "una reserva sin pagar retiene butacas pero no vendió ninguna entrada.")));
        contenido.add(Box.createVerticalStrut(8));

        List<Map.Entry<String, Total>> tarifas = List.copyOf(bordero.porTarifa().entrySet());
        if (tarifas.isEmpty()) {
            contenido.add(Componentes.izquierda(Componentes.nota("Todavía no se cobró ninguna entrada de esta "
                    + "función. No es un error: es un borderó en cero, y se puede declarar igual.")));
        } else {
            Tabla<Map.Entry<String, Total>> tabla = new Tabla<>(
                    Columna.<Map.Entry<String, Total>>de("Tarifa", e -> etiqueta(e.getKey())),
                    Columna.<Map.Entry<String, Total>>numero("Entradas", e -> e.getValue().cantidad()),
                    Columna.<Map.Entry<String, Total>>numero("Total", e -> precio(e.getValue().total())));
            tabla.mostrar(tarifas);
            var scroll = tabla.conScroll();
            scroll.setPreferredSize(new Dimension(300, Tabla.altoPara(tarifas.size())));
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tabla.altoPara(tarifas.size())));
            contenido.add(Componentes.izquierda(scroll));
            contenido.add(Componentes.izquierda(new JLabel(bordero.espectadores() + " espectadores")));
        }
        contenido.add(Box.createVerticalStrut(8));
        contenido.add(Componentes.izquierda(Componentes.renglon("Recaudación bruta",
                precio(bordero.recaudacionBruta()), false)));
        contenido.add(Componentes.izquierda(Componentes.renglon("Descuentos", "− " + precio(bordero.descuentos()),
                false)));
        contenido.add(Componentes.izquierda(new JSeparator()));
        contenido.add(Componentes.izquierda(Componentes.renglon("Recaudación neta",
                precio(bordero.recaudacionNeta()), true)));
        contenido.add(Componentes.izquierda(Componentes.nota("Las tres van separadas porque cuentan cosas distintas: "
                + "la bruta es a precio de lista, los descuentos son lo que resignó el cine por una promoción suya, y "
                + "la neta es lo que entró en la caja.")));
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
        contenido.add(Componentes.izquierda(fila));
        contenido.add(Componentes.izquierda(Componentes.nota("Emitir vuelve a pedir el borderó y lo guarda como "
                + "texto en esta PC. El de una función es uno solo y vale el último, porque las entradas se siguen "
                + "vendiendo hasta que la película arranca.")));
        contenido.add(Box.createVerticalGlue());
        contenido.revalidate();
        contenido.repaint();
    }

    // Se pide de nuevo al emitir: lo que está en pantalla puede haber quedado viejo.
    private void emitir() {
        cargar(() -> new Emision(apiInformes.obtenerBordero(funcionId),
                apiCatalogos.obtenerTarifas().stream().map(Tarifa::nombre).toList()), emision -> {
            pintar(emision.bordero(), true);
            JFileChooser elegir = new JFileChooser();
            elegir.setSelectedFile(new File(BorderoTxt.nombreArchivo(funcionId)));
            if (elegir.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path destino = elegir.getSelectedFile().toPath();
            try {
                Files.writeString(destino, BorderoTxt.escribir(emision.bordero(), emision.tarifas()),
                        StandardCharsets.UTF_8);
            } catch (IOException ex) {
                Mensajes.error(this, new ErrorApi(-1, "No se pudo guardar el borderó: " + ex.getMessage()));
                return;
            }
            avisar("Borderó emitido: " + emision.bordero().espectadores() + " espectadores, "
                    + precio(emision.bordero().recaudacionNeta()) + ".\nGuardado en " + destino);
        });
    }
}
