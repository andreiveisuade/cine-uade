package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.PedidoSala;
import ar.uade.cine.swing.api.dto.Sala;
import ar.uade.cine.swing.api.dto.TipoSala;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tabla.Columna;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

final class PantallaSalas extends Pantalla {

    private record Datos(List<Sala> salas, List<TipoSala> tipos) {
    }

    private final Navegacion navegacion;
    private final Tabla<Sala> tabla = new Tabla<>(
            Columna.<Sala>de("Sala", Sala::nombre).ancho(160),
            Columna.<Sala>de("Tipo", s -> etiqueta(s.tipo())),
            Columna.<Sala>de("Distribución", s -> s.butacasPorFila().stream().map(String::valueOf)
                    .collect(Collectors.joining(","))).ancho(180),
            Columna.<Sala>numero("Butacas", Sala::capacidadSala),
            Columna.<Sala>numero("Limpieza", s -> s.minutosLimpieza() + " min"));

    private final JTextField nombre = new JTextField();
    private final JComboBox<Opcion<String>> tipo = new JComboBox<>();
    private final JTextField distribucion = new JTextField();
    private final JLabel resumenDistribucion = new JLabel(" ");
    private final JTextField vip = new JTextField();
    private final JTextField pareja = new JTextField();
    private final JTextField accesibles = new JTextField();
    private final JTextField limpieza = new JTextField("15");

    PantallaSalas(ApiHttp api, Navegacion navegacion) {
        super(api, "Salas", "Doble clic en una sala abre su mapa, para marcar butacas fuera de servicio.");
        this.navegacion = navegacion;

        JButton butacas = new JButton("Butacas");
        JButton borrar = new JButton("Borrar");
        butacas.addActionListener(e -> tabla.seleccionada().ifPresent(this::abrirMapa));
        borrar.addActionListener(e -> tabla.seleccionada().ifPresent(this::borrar));
        tabla.alDobleClic(this::abrirMapa);
        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(butacas);
        acciones.add(borrar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(acciones, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(formulario(), BorderLayout.EAST);
        recargar();
    }

    private JPanel formulario() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        for (JTextField campo : List.of(distribucion, vip, pareja, accesibles)) campo.setFont(mono);
        distribucion.setToolTipText("Una fila por número, separadas por coma. La primera es la A. Ej: 8,10,12,12,14");
        vip.setToolTipText("Ej: I1,I2,J1");
        pareja.setToolTipText("Ej: A1,A2");
        accesibles.setToolTipText("Ej: A1,A8");
        resumenDistribucion.setForeground(Componentes.gris());
        distribucion.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resumir();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resumir();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resumir();
            }
        });
        JButton crear = new JButton("Crear sala");
        crear.addActionListener(e -> crear());

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(Componentes.subtitulo("Nueva sala"))
                .campo("Nombre", nombre)
                .campo("Tipo", tipo)
                .campo("Butacas por fila", distribucion)
                .ancho(resumenDistribucion)
                .campo("Butacas VIP", vip)
                .campo("Butacas de pareja", pareja)
                .campo("Butacas accesibles", accesibles)
                .campo("Minutos de limpieza", limpieza)
                .ancho(Componentes.nota("Lo que hay que esperar entre dos funciones. Una sala chica se levanta más "
                        + "rápido."))
                .ancho(crear)
                .cerrar();
        JPanel panel = Componentes.conBorde(formulario);
        panel.setPreferredSize(new Dimension(360, 0));
        return panel;
    }

    // Cuenta filas y butacas mientras se tipea: es aritmética sobre lo tipeado, no una regla.
    private void resumir() {
        List<Integer> filas = Campos.enteros(distribucion);
        if (distribucion.getText().isBlank() || filas.isEmpty()) {
            resumenDistribucion.setText(" ");
            return;
        }
        resumenDistribucion.setText(filas.size() + " filas (A–" + (char) ('A' + filas.size() - 1) + "), "
                + filas.stream().mapToInt(Integer::intValue).sum() + " butacas");
    }

    private void recargar() {
        cargar(() -> new Datos(api.obtenerSalas(), api.obtenerTiposSala()), datos -> {
            tabla.mostrar(datos.salas());
            if (tipo.getItemCount() == 0) {
                datos.tipos().forEach(t -> tipo.addItem(new Opcion<>(t.nombre(),
                        etiqueta(t.nombre()) + " (×" + t.multiplicador() + ")")));
            }
        });
    }

    private void crear() {
        PedidoSala pedido = new PedidoSala(nombre.getText().trim(), Campos.elegido(tipo), Campos.enteros(distribucion),
                Campos.codigos(vip), Campos.codigos(pareja), Campos.codigos(accesibles), Campos.entero(limpieza));
        cargar(() -> api.crearSala(pedido), creada -> {
            avisar(creada.nombre() + " creada con " + creada.capacidadSala() + " butacas");
            for (JTextField campo : List.of(nombre, distribucion, vip, pareja, accesibles)) campo.setText("");
            limpieza.setText("15");
            recargar();
        });
    }

    private void borrar(Sala sala) {
        if (!confirmar("¿Borrar " + sala.nombre() + "?")) return;
        accion(() -> {
            api.eliminarSala(sala.id());
            return null;
        }, "Sala borrada", this::recargar);
    }

    private void abrirMapa(Sala sala) {
        navegacion.abrir(new PantallaMapaSala(api, navegacion, sala.id()));
    }
}
