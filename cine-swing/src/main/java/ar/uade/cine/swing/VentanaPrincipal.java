package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Etiquetas;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.produccion.VentanaProduccion;
import com.formdev.flatlaf.FlatClientProperties;

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
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

// El marco del panel: cabecera, menú según el rol, la pantalla elegida y la barra de estado.
// Cada entrada del menú crea la pantalla de nuevo, así muestra datos frescos; cambiar el tema rearma el marco.
public final class VentanaPrincipal extends JFrame implements Navegacion {

    /** En las Preferences: «claro» u «oscuro», lo último que eligió el encargado con el interruptor. */
    static final String CLAVE_TEMA = "tema";

    private record Grupo(String titulo, List<Destino> destinos) {
    }

    private static final List<Grupo> MENU = List.of(
            new Grupo("Cartelera", List.of(Destino.PELICULAS, Destino.POR_REVISAR, Destino.IMPORTADOR)),
            new Grupo("Programación", List.of(Destino.SALAS, Destino.FUNCIONES, Destino.GRILLA, Destino.PLANIFICADOR,
                    Destino.AGENDA)),
            new Grupo("Ventas", List.of(Destino.RESERVAS, Destino.PROMOCIONES, Destino.CANDY, Destino.CAJA,
                    Destino.DECLARACION_JURADA, Destino.CLIENTES)),
            new Grupo("Acceso", List.of(Destino.PUERTA)));

    private final Pantallas pantallas;
    private final Servidor servidor;
    private final Empleado empleado;
    private final Preferences preferencias;
    private final Runnable alSalir;
    private final JPanel contenido = new JPanel(new BorderLayout());
    private final Map<Destino, JToggleButton> botones = new EnumMap<>(Destino.class);
    private Destino destino;
    private Supplier<JComponent> actual;

    public VentanaPrincipal(Apis apis, Servidor servidor, Empleado empleado, Preferences preferencias,
                            Runnable alSalir) {
        super("Cine UADE · " + servidor.marca() + " · " + empleado.nombre());
        this.pantallas = new Pantallas(apis, this);
        this.servidor = servidor;
        this.empleado = empleado;
        this.preferencias = preferencias;
        this.alSalir = alSalir;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        armar();
        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        ir(empleado.esAdministrador() ? Destino.PELICULAS : Destino.PUERTA);
    }

    // Los bordes y fondos leen Colores al crearse: con otro tema hay que volver a crearlos, no solo redibujarlos.
    private void armar() {
        botones.clear();
        JPanel raiz = new JPanel(new BorderLayout());
        // Producción salta a la vista: un alta de prueba ahí la ve el público.
        if (servidor.entorno() == Servidor.Entorno.PRODUCCION) {
            raiz.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, Colores.error()));
        }
        raiz.add(cabecera(), BorderLayout.NORTH);
        // El rol no es cosmético: el acomodador ni ve el resto del menú, y el backend además le cierra las rutas.
        List<Grupo> menu = empleado.esAdministrador() ? MENU
                : List.of(new Grupo("Acceso", List.of(Destino.PUERTA)));
        JPanel lista = menu(menu);
        JScrollPane lateral = new JScrollPane(lista);
        lateral.getViewport().setBackground(Colores.lateral());
        lateral.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colores.borde()));
        lateral.setPreferredSize(new Dimension(188, 0));
        raiz.add(lateral, BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        raiz.add(pie(), BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    private JPanel cabecera() {
        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.X_AXIS));
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Colores.borde()),
                BorderFactory.createEmptyBorder(Componentes.JUNTO, Componentes.APARTE, Componentes.JUNTO,
                        Componentes.APARTE)));
        JLabel marca = new JLabel("CINE UADE");
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, Componentes.LETRA_TARJETA));
        cabecera.add(marca);
        cabecera.add(Box.createHorizontalGlue());
        cabecera.add(marca(servidor));
        cabecera.add(Box.createHorizontalStrut(Componentes.CERCA));
        JLabel quien = new JLabel(empleado.nombre() + " · " + Etiquetas.etiqueta(empleado.rol()));
        quien.setForeground(Colores.secundario());
        cabecera.add(quien);
        cabecera.add(Box.createHorizontalStrut(Componentes.CERCA));
        cabecera.add(interruptorDeTema());
        cabecera.add(Box.createHorizontalStrut(Componentes.JUNTO));
        JButton produccion = new JButton("Estado de producción");
        produccion.addActionListener(e -> VentanaProduccion.abrir(this, preferencias));
        cabecera.add(produccion);
        cabecera.add(Box.createHorizontalStrut(Componentes.JUNTO));
        JButton salir = new JButton("Salir");
        salir.addActionListener(e -> alSalir.run());
        cabecera.add(salir);
        return cabecera;
    }

    // Dice adónde lleva, no dónde está: «Tema claro» estando en oscuro.
    private JButton interruptorDeTema() {
        boolean oscuro = Colores.oscuro();
        JButton tema = new JButton(oscuro ? "☀ Tema claro" : "☾ Tema oscuro");
        tema.setToolTipText("Se recuerda para la próxima vez");
        tema.addActionListener(e -> {
            Colores.usarTema(!oscuro);
            preferencias.put(CLAVE_TEMA, oscuro ? "claro" : "oscuro");
            armar();
            botones.get(destino).setSelected(true);
            mostrar(actual);
        });
        return tema;
    }

    private JComponent pie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colores.borde()));
        JLabel barra = Mensajes.barraDeEstado(getRootPane());
        barra.setBorder(BorderFactory.createEmptyBorder(6, Componentes.APARTE, 6, Componentes.APARTE));
        JLabel donde = new JLabel(servidor.url());
        donde.setForeground(Colores.secundario());
        donde.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Componentes.APARTE));
        pie.add(barra, BorderLayout.CENTER);
        pie.add(donde, BorderLayout.EAST);
        return pie;
    }

    // Producción salta a la vista: un alta de prueba ahí la ve el público.
    static JLabel marca(Servidor servidor) {
        JLabel marca = new JLabel(servidor.entorno() == Servidor.Entorno.PRODUCCION
                ? servidor.marca() + " · lo que cambies lo ve el público" : servidor.marca());
        marca.setToolTipText(servidor.url() + " · para cambiarlo, salí y elegilo al ingresar");
        marca.setBorder(BorderFactory.createEmptyBorder(2, Componentes.JUNTO, 2, Componentes.JUNTO));
        switch (servidor.entorno()) {
            case LOCAL -> marca.setForeground(Colores.secundario());
            case PRODUCCION -> {
                marca.setForeground(Colores.error());
                marca.setFont(marca.getFont().deriveFont(Font.BOLD));
                marca.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Colores.error(), 1, true),
                        BorderFactory.createEmptyBorder(2, Componentes.JUNTO, 2, Componentes.JUNTO)));
            }
            case OTRO -> {
                marca.setForeground(Colores.aviso());
                marca.setFont(marca.getFont().deriveFont(Font.BOLD));
            }
        }
        return marca;
    }

    private JPanel menu(List<Grupo> grupos) {
        JPanel menu = new JPanel();
        menu.setBackground(Colores.lateral());
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(Componentes.JUNTO, Componentes.JUNTO, Componentes.JUNTO,
                Componentes.JUNTO));
        ButtonGroup grupo = new ButtonGroup();
        for (Grupo g : grupos) {
            JLabel titulo = new JLabel(g.titulo().toUpperCase());
            titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, Componentes.LETRA_ROTULO));
            titulo.setForeground(Colores.secundario());
            titulo.setBorder(BorderFactory.createEmptyBorder(Componentes.CERCA, Componentes.JUNTO, Componentes.PEGADO,
                    0));
            menu.add(Componentes.izquierda(titulo));
            for (Destino d : g.destinos()) {
                // Botón tipo pestaña con la raya a la izquierda: la misma marca ámbar que la pestaña elegida.
                JToggleButton boton = new JToggleButton(d.titulo());
                boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TAB);
                boton.putClientProperty(FlatClientProperties.TAB_BUTTON_UNDERLINE_PLACEMENT, SwingConstants.LEFT);
                boton.setHorizontalAlignment(SwingConstants.LEFT);
                boton.setBorder(BorderFactory.createEmptyBorder(6, Componentes.CERCA, 6, Componentes.JUNTO));
                boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, boton.getPreferredSize().height));
                boton.addActionListener(e -> ir(d));
                grupo.add(boton);
                botones.put(d, boton);
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
        this.destino = destino;
        mostrar(() -> pantallas.de(destino));
    }

    @Override
    public void volver() {
        ir(destino);
    }

    @Override
    public void abrirInforme(int funcionId) {
        mostrar(() -> pantallas.informe(funcionId));
    }

    @Override
    public void abrirCobro(int reservaId) {
        mostrar(() -> pantallas.cobro(reservaId));
    }

    @Override
    public void abrirMapa(int salaId) {
        mostrar(() -> pantallas.mapa(salaId));
    }

    private void mostrar(Supplier<JComponent> pantalla) {
        actual = pantalla;
        Componentes.reemplazar(contenido, pantalla.get());
    }
}
