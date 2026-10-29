package ar.uade.cine.swing.pantallas.salas;

import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.salas.Asiento;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.MapaButacas.Estilo;
import ar.uade.cine.swing.comun.MapaButacas;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.escapar;

// Mapa de una sala para marcar y reponer butacas (R9): no hay ocupación, es el estado físico del asiento.
public final class PantallaMapaSala extends Pantalla {

    private final ApiSalas apiSalas;
    private final int salaId;
    private final JLabel resumen = new JLabel(" ");
    private final JPanel mapa = new JPanel(new BorderLayout());

    public PantallaMapaSala(ApiSalas apiSalas, Navegacion navegacion, int salaId) {
        super("Butacas", "Clic en una butaca para marcarla fuera de servicio o reponerla. Una butaca rota no se "
                + "vende en ninguna función.");
        this.apiSalas = apiSalas;
        this.salaId = salaId;

        JButton volver = Componentes.deTexto(new JButton("← Salas"));
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
        cargar(() -> apiSalas.obtenerSala(salaId), this::pintar);
    }

    private void pintar(Sala sala) {
        long rotas = sala.asientos().stream().filter(Asiento::fueraDeServicio).count();
        resumen.setText("<html><span style='font-size:16pt'><b>" + escapar(sala.nombre()) + "</b></span><br>"
                + etiqueta(sala.tipo()) + " · " + sala.filas() + " filas · " + sala.capacidadSala() + " butacas · "
                + rotas + " fuera de servicio</html>");
        Componentes.reemplazar(mapa, new MapaButacas(sala.filas(), sala.asientos(), this::estilo, this::alternar));
    }

    private Estilo estilo(Asiento asiento) {
        if (asiento.fueraDeServicio()) {
            return new Estilo(Colores.butacaFueraDeServicio(), Colores.borde(), true, true,
                    asiento.codigo() + " · fuera de servicio · clic para reponer");
        }
        return new Estilo(Colores.butaca(asiento.tipo()), Colores.borde(), true, false,
                asiento.codigo() + " · " + etiqueta(asiento.tipo()) + " · clic para marcar fuera de servicio");
    }

    private void alternar(Asiento asiento) {
        String nuevo = asiento.fueraDeServicio() ? "HABILITADO" : "FUERA_DE_SERVICIO";
        cargar(() -> apiSalas.cambiarEstadoAsiento(salaId, asiento.codigo(), nuevo), this::pintar);
    }

    private static JPanel referencia() {
        JPanel panel = new JPanel(new FlujoConSalto());
        panel.add(muestra(Colores.butaca("ESTANDAR"), "disponible"));
        panel.add(muestra(Colores.butacaFueraDeServicio(), "fuera de servicio"));
        for (String tipo : List.of("VIP", "PAREJA", "ACCESIBLE")) {
            panel.add(muestra(Colores.butaca(tipo), MapaButacas.SIMBOLO.get(tipo) + " " + etiqueta(tipo)));
        }
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
        etiqueta.setForeground(Colores.secundario());
        return etiqueta;
    }
}
