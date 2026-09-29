package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Etiquetas;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.candy.PantallaCandy;
import ar.uade.cine.swing.pantallas.cartelera.PantallaImportador;
import ar.uade.cine.swing.pantallas.cartelera.PantallaPeliculas;
import ar.uade.cine.swing.pantallas.cartelera.PantallaPendientes;
import ar.uade.cine.swing.pantallas.funciones.PantallaAgenda;
import ar.uade.cine.swing.pantallas.funciones.PantallaFunciones;
import ar.uade.cine.swing.pantallas.informes.PantallaCaja;
import ar.uade.cine.swing.pantallas.informes.PantallaDeclaracionJurada;
import ar.uade.cine.swing.pantallas.informes.PantallaInformeFuncion;
import ar.uade.cine.swing.pantallas.programaciones.PantallaPlanificador;
import ar.uade.cine.swing.pantallas.programaciones.PantallaProgramaciones;
import ar.uade.cine.swing.pantallas.promociones.PantallaPromociones;
import ar.uade.cine.swing.pantallas.salas.PantallaMapaSala;
import ar.uade.cine.swing.pantallas.salas.PantallaSalas;
import ar.uade.cine.swing.pantallas.ventas.PantallaCobro;
import ar.uade.cine.swing.pantallas.ventas.PantallaPuerta;
import ar.uade.cine.swing.pantallas.ventas.PantallaReservas;

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
import java.util.function.Supplier;

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

    private final Apis apis;
    private final JPanel contenido = new JPanel(new BorderLayout());
    private final Map<Destino, JToggleButton> botones = new EnumMap<>(Destino.class);
    private final ButtonGroup grupo = new ButtonGroup();

    public VentanaPrincipal(Apis apis, String servidor, Empleado empleado, Runnable alSalir) {
        super("Cine UADE · " + empleado.nombre());
        this.apis = apis;
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
        mostrar(crear(destino).get());
    }

    @Override
    public void abrirInforme(int funcionId) {
        mostrar(new PantallaInformeFuncion(apis.catalogos(), apis.funciones(), apis.informes(), this, funcionId));
    }

    @Override
    public void abrirCobro(int reservaId) {
        mostrar(new PantallaCobro(apis.catalogos(), apis.ventas(), this, reservaId));
    }

    @Override
    public void abrirMapa(int salaId) {
        mostrar(new PantallaMapaSala(apis.salas(), this, salaId));
    }

    private void mostrar(JComponent pantalla) {
        Componentes.reemplazar(contenido, pantalla);
    }

    // Sin default a propósito: un destino nuevo sin pantalla no compila.
    private Supplier<JComponent> crear(Destino destino) {
        return switch (destino) {
            case PELICULAS -> () -> new PantallaPeliculas(apis.cartelera(), apis.catalogos());
            case POR_REVISAR -> () -> new PantallaPendientes(apis.cartelera(), this);
            case IMPORTADOR -> () -> new PantallaImportador(apis.cartelera(), this);
            case SALAS -> () -> new PantallaSalas(apis.catalogos(), apis.salas(), this);
            case FUNCIONES -> () -> new PantallaFunciones(apis.cartelera(), apis.catalogos(), apis.funciones(),
                    apis.salas(), this);
            case GRILLA -> () -> new PantallaProgramaciones(apis.cartelera(), apis.catalogos(), apis.programaciones(),
                    apis.salas());
            case PLANIFICADOR -> () -> new PantallaPlanificador(apis.catalogos(), apis.programaciones());
            case AGENDA -> () -> new PantallaAgenda(apis.funciones(), apis.salas(), this);
            case RESERVAS -> () -> new PantallaReservas(apis.ventas(), this);
            case PROMOCIONES -> () -> new PantallaPromociones(apis.catalogos(), apis.promociones());
            case CANDY -> () -> new PantallaCandy(apis.candy(), apis.catalogos(), apis.clientes());
            case CAJA -> () -> new PantallaCaja(apis.informes());
            case DECLARACION_JURADA -> () -> new PantallaDeclaracionJurada(apis.catalogos(), apis.informes());
            case PUERTA -> () -> new PantallaPuerta(apis.catalogos(), apis.ventas());
        };
    }
}
