package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Etiquetas;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// El marco del panel: cabecera, menú según el rol y la pantalla elegida, recreada para mostrar datos frescos.
/**
 * El marco del panel: cabecera, menú lateral y la pantalla elegida. El acomodador solo ve Puerta; cada
 * vez que se elige una entrada la pantalla se crea de nuevo, que es lo que hace que muestre datos frescos.
 */
public final class VentanaPrincipal extends JFrame implements Navegacion {

    private record Grupo(String titulo, List<Destino> destinos) {
    }

    private static final List<Grupo> MENU = List.of(
            new Grupo("Cartelera", List.of(Destino.PELICULAS, Destino.POR_REVISAR, Destino.IMPORTADOR)),
            new Grupo("Programación", List.of(Destino.SALAS, Destino.FUNCIONES, Destino.GRILLA, Destino.PLANIFICADOR,
                    Destino.AGENDA)),
            new Grupo("Ventas", List.of(Destino.RESERVAS, Destino.PROMOCIONES, Destino.CANDY, Destino.CAJA,
                    Destino.DECLARACION_JURADA)),
            new Grupo("Acceso", List.of(Destino.PUERTA)));

    private final Pantallas pantallas;
    private final JPanel contenido = new JPanel(new BorderLayout());
    private final Map<Destino, JToggleButton> botones = new EnumMap<>(Destino.class);
    private final ButtonGroup grupo = new ButtonGroup();

    public VentanaPrincipal(Apis apis, String servidor, Empleado empleado, Runnable alSalir) {
        super("Cine UADE · " + empleado.nombre());
        this.pantallas = new Pantallas(apis, this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera(empleado, servidor, alSalir), BorderLayout.NORTH);
        // El rol no es cosmético: el acomodador ni ve el resto del menú, y el backend además le cierra las rutas.
        List<Grupo> menu = empleado.esAdministrador() ? MENU
                : List.of(new Grupo("Acceso", List.of(Destino.PUERTA)));
        JScrollPane lateral = new JScrollPane(menu(menu));
        lateral.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colores.secundario()));
        lateral.setPreferredSize(new Dimension(180, 0));
        raiz.add(lateral, BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        raiz.add(Mensajes.barraDeEstado(getRootPane()), BorderLayout.SOUTH);
        setContentPane(raiz);

        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        ir(empleado.esAdministrador() ? Destino.PELICULAS : Destino.PUERTA);
    }

    private JPanel cabecera(Empleado empleado, String servidor, Runnable alSalir) {
        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.X_AXIS));
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Colores.secundario()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        JLabel marca = new JLabel("CINE UADE");
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 18f));
        cabecera.add(marca);
        cabecera.add(Box.createHorizontalStrut(12));
        cabecera.add(new JLabel(Etiquetas.etiqueta(empleado.rol())));
        cabecera.add(Box.createHorizontalGlue());
        cabecera.add(new JLabel(empleado.nombre() + "  ·  " + servidor));
        cabecera.add(Box.createHorizontalStrut(12));
        JButton salir = new JButton("Salir");
        salir.addActionListener(e -> alSalir.run());
        cabecera.add(salir);
        return cabecera;
    }

    private JPanel menu(List<Grupo> grupos) {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Grupo g : grupos) {
            JLabel titulo = new JLabel(g.titulo().toUpperCase());
            titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 11f));
            titulo.setForeground(Colores.secundario());
            titulo.setBorder(BorderFactory.createEmptyBorder(12, 6, 4, 0));
            menu.add(Componentes.izquierda(titulo));
            for (Destino destino : g.destinos()) {
                JToggleButton boton = new JToggleButton(destino.titulo());
                boton.setHorizontalAlignment(JToggleButton.LEFT);
                boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, boton.getPreferredSize().height));
                boton.putClientProperty("JButton.buttonType", "toolBarButton");
                boton.addActionListener(e -> ir(destino));
                grupo.add(boton);
                botones.put(destino, boton);
                menu.add(Componentes.izquierda(boton));
            }
        }
        return menu;
    }

    @Override
    public void ir(Destino destino) {
        // Sin botón es un destino fuera del menú de este rol: el acomodador solo tiene Puerta.
        JToggleButton boton = botones.get(destino);
        if (boton == null) return;
        boton.setSelected(true);
        mostrar(pantallas.de(destino));
    }

    @Override
    public void abrirInforme(int funcionId) {
        mostrar(pantallas.informe(funcionId));
    }

    @Override
    public void abrirCobro(int reservaId) {
        mostrar(pantallas.cobro(reservaId));
    }

    @Override
    public void abrirMapa(int salaId) {
        mostrar(pantallas.mapa(salaId));
    }

    private void mostrar(JComponent pantalla) {
        Componentes.reemplazar(contenido, pantalla);
    }
}
