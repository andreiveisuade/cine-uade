package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Empleado;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Etiquetas;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * El marco del panel: cabecera, menú lateral y la pantalla elegida. El menú es el de {@code admin/AppAdmin.jsx}; cada
 * vez que se elige una entrada la pantalla se crea de nuevo, que es lo que hace que muestre datos frescos.
 */
public final class VentanaPrincipal extends JFrame implements Navegacion {

    private record Grupo(String titulo, List<String> destinos) {
    }

    private static final List<Grupo> MENU = List.of(
            new Grupo("Cartelera", List.of("Películas", "Por revisar", "Importador")),
            new Grupo("Programación", List.of("Salas", "Funciones", "Grilla", "Planificador", "Agenda")),
            new Grupo("Ventas", List.of("Reservas", "Promociones", "Candy", "Caja", "Declaración jurada")),
            new Grupo("Acceso", List.of("Puerta")));

    private final ApiHttp api;
    private final JPanel contenido = new JPanel(new BorderLayout());
    private final Map<String, JToggleButton> botones = new LinkedHashMap<>();
    private final ButtonGroup grupo = new ButtonGroup();

    public VentanaPrincipal(ApiHttp api, Empleado empleado, Runnable alSalir) {
        super("Cine UADE · " + empleado.nombre());
        this.api = api;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera(empleado, alSalir), BorderLayout.NORTH);
        // El rol no es cosmético: el acomodador ni ve el resto del menú, y el backend además le cierra las rutas.
        List<Grupo> menu = empleado.esAdministrador() ? MENU
                : List.of(new Grupo("Acceso", List.of("Puerta")));
        JScrollPane lateral = new JScrollPane(menu(menu));
        lateral.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Componentes.gris()));
        lateral.setPreferredSize(new Dimension(200, 0));
        raiz.add(lateral, BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        setContentPane(raiz);

        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        ir(empleado.esAdministrador() ? "Películas" : "Puerta");
    }

    private JPanel cabecera(Empleado empleado, Runnable alSalir) {
        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.X_AXIS));
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Componentes.gris()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        JLabel marca = new JLabel("CINE UADE");
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 18f));
        cabecera.add(marca);
        cabecera.add(Box.createHorizontalStrut(12));
        cabecera.add(new JLabel(Etiquetas.etiqueta(empleado.rol())));
        cabecera.add(Box.createHorizontalGlue());
        cabecera.add(new JLabel(empleado.nombre() + "  ·  " + api.urlBase()));
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
            titulo.setForeground(Componentes.gris());
            titulo.setBorder(BorderFactory.createEmptyBorder(12, 6, 4, 0));
            menu.add(Componentes.izquierda(titulo));
            for (String destino : g.destinos()) {
                JToggleButton boton = new JToggleButton(destino);
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
    public void ir(String destino) {
        JToggleButton boton = botones.get(destino);
        if (boton == null) return;
        boton.setSelected(true);
        mostrar(crear(destino).get());
    }

    @Override
    public void abrir(JComponent pantalla) {
        mostrar(pantalla);
    }

    private void mostrar(JComponent pantalla) {
        contenido.removeAll();
        contenido.add(pantalla);
        contenido.revalidate();
        contenido.repaint();
    }

    private Supplier<JComponent> crear(String destino) {
        return switch (destino) {
            case "Películas" -> () -> new PantallaPeliculas(api, this);
            case "Por revisar" -> () -> new PantallaPendientes(api, this);
            case "Importador" -> () -> new PantallaImportador(api, this);
            case "Salas" -> () -> new PantallaSalas(api, this);
            case "Funciones" -> () -> new PantallaFunciones(api, this);
            case "Grilla" -> () -> new PantallaProgramaciones(api, this);
            case "Planificador" -> () -> new PantallaPlanificador(api, this);
            case "Agenda" -> () -> new PantallaAgenda(api, this);
            case "Reservas" -> () -> new PantallaReservas(api, this);
            case "Promociones" -> () -> new PantallaPromociones(api, this);
            case "Candy" -> () -> new PantallaCandy(api, this);
            case "Caja" -> () -> new PantallaCaja(api);
            case "Declaración jurada" -> () -> new PantallaDeclaracionJurada(api, this);
            case "Puerta" -> () -> new PantallaPuerta(api);
            default -> throw new IllegalArgumentException("El menú no tiene la pantalla " + destino);
        };
    }
}
