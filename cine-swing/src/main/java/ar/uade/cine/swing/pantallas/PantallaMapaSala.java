package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Asiento;
import ar.uade.cine.swing.api.dto.Sala;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.MapaButacas.Estilo;
import ar.uade.cine.swing.comun.MapaButacas;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

/** Mapa de la sala para marcar y reponer butacas (R9): acá no hay ocupación, es el estado físico del asiento. */
final class PantallaMapaSala extends Pantalla {

    private final int salaId;
    private final JLabel resumen = new JLabel(" ");
    private final JPanel mapa = new JPanel(new BorderLayout());

    PantallaMapaSala(ApiHttp api, Navegacion navegacion, int salaId) {
        super(api, "Butacas", "Clic en una butaca para marcarla fuera de servicio o reponerla. Una butaca rota no se "
                + "vende en ninguna función.");
        this.salaId = salaId;
        JButton volver = new JButton("← Salas");
        volver.addActionListener(e -> navegacion.ir(Destino.SALAS));
        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.add(volver, BorderLayout.WEST);
        norte.add(resumen, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.add(norte, BorderLayout.NORTH);
        centro.add(new JScrollPane(mapa), BorderLayout.CENTER);
        centro.add(referencia(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        recargar();
    }

    private void recargar() {
        cargar(() -> api.obtenerSala(salaId), this::pintar);
    }

    private void pintar(Sala sala) {
        long rotas = sala.asientos().stream().filter(a -> "FUERA_DE_SERVICIO".equals(a.estado())).count();
        resumen.setText("<html><span style='font-size:16pt'><b>" + sala.nombre() + "</b></span><br>"
                + etiqueta(sala.tipo()) + " · " + sala.filas() + " filas · " + sala.capacidadSala() + " butacas · "
                + rotas + " fuera de servicio</html>");
        mapa.removeAll();
        mapa.add(new MapaButacas(sala.filas(), sala.asientos(), this::estilo, this::alternar));
        mapa.revalidate();
        mapa.repaint();
    }

    private Estilo estilo(Asiento asiento) {
        if ("FUERA_DE_SERVICIO".equals(asiento.estado())) {
            return new Estilo(Colores.butacaFueraDeServicio(), Colores.borde(), true, true,
                    asiento.codigo() + " · fuera de servicio · clic para reponer");
        }
        return new Estilo(MapaButacas.colorTipo(asiento.tipo()), Colores.borde(), true, false,
                asiento.codigo() + " · " + etiqueta(asiento.tipo()) + " · clic para marcar fuera de servicio");
    }

    private void alternar(Asiento asiento) {
        String nuevo = "FUERA_DE_SERVICIO".equals(asiento.estado()) ? "HABILITADO" : "FUERA_DE_SERVICIO";
        cargar(() -> api.cambiarEstadoAsiento(salaId, asiento.codigo(), nuevo), hecho -> recargar());
    }

    private static JPanel referencia() {
        JPanel panel = new JPanel(new FlujoConSalto());
        panel.add(muestra(MapaButacas.colorTipo("ESTANDAR"), "disponible"));
        panel.add(muestra(Colores.butacaFueraDeServicio(), "fuera de servicio"));
        panel.add(muestra(MapaButacas.colorTipo("VIP"), "* VIP"));
        panel.add(muestra(MapaButacas.colorTipo("PAREJA"), "& pareja"));
        panel.add(muestra(MapaButacas.colorTipo("ACCESIBLE"), "+ accesible"));
        return panel;
    }

    private static JLabel muestra(Color color, String texto) {
        JLabel etiqueta = new JLabel("  " + texto);
        etiqueta.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRect(x, y, 12, 12);
                g.setColor(Colores.borde());
                g.drawRect(x, y, 12, 12);
            }

            @Override
            public int getIconWidth() {
                return 13;
            }

            @Override
            public int getIconHeight() {
                return 13;
            }
        });
        etiqueta.setForeground(Componentes.gris());
        return etiqueta;
    }
}
