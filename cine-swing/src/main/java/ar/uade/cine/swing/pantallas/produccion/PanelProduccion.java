package ar.uade.cine.swing.pantallas.produccion;

import ar.uade.cine.swing.api.ApiAiven;
import ar.uade.cine.swing.api.ApiRender;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.EstadoServicio;
import ar.uade.cine.swing.api.EstadoServicio.Fase;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

// Cómo están Render y Aiven, y el botón que los despierta en orden: primero la base, después la app.
// Sondea con un pedido por vez (comun.Tarea) y un Timer entre uno y otro, así el EDT nunca espera.
public final class PanelProduccion extends JPanel {

    // Render Free tarda hasta ~9 min en despertar; pasado el plazo se deja de insistir.
    private static final Duration PLAZO = Duration.ofMinutes(12);
    private static final Duration ESPERA_RENDER = Duration.ofSeconds(10);

    private final Preferences preferencias;
    private final UnaryOperator<String> sistema;
    private final String urlRender;
    private final String urlAiven;
    private final Duration intervalo;

    private final JLabel render = new JLabel(" ");
    private final JLabel aiven = new JLabel(" ");
    private final JLabel mensaje = Componentes.texto(" ");
    private final JButton actualizar = Componentes.primario(new JButton("Actualizar"));
    private final JButton despertar = new JButton("Despertar");
    private final JButton consola = new JButton("Abrir consola de Aiven");
    private final JButton configurar = new JButton("Configurar Aiven…");
    private final JButton olvidar = new JButton("Olvidar token");
    private Timer proximo;
    private Instant limite;
    private boolean cerrado;

    public PanelProduccion(Preferences preferencias) {
        this(preferencias, CuentaAiven.DEL_SISTEMA, Servidor.PRODUCCION.url(), ApiAiven.URL,
                Duration.ofSeconds(10));
    }

    PanelProduccion(Preferences preferencias, UnaryOperator<String> sistema, String urlRender, String urlAiven,
                    Duration intervalo) {
        super(new BorderLayout());
        this.preferencias = preferencias;
        this.sistema = sistema;
        this.urlRender = urlRender;
        this.urlAiven = urlAiven;
        this.intervalo = intervalo;

        add(new Formulario()
                .ancho(Componentes.encabezado("Estado de producción",
                        "Render y Aiven apagan lo que no se usa: la base se enciende primero y la app después"))
                .campo("Render (la app)", render)
                .campo("Aiven (la base)", aiven)
                .ancho(mensaje)
                .ancho(Componentes.botones(actualizar, despertar))
                .ancho(Componentes.botones(configurar, olvidar, consola))
                .cerrar());

        actualizar.addActionListener(e -> actualizar());
        despertar.addActionListener(e -> despertar());
        consola.addActionListener(e -> abrirConsola());
        configurar.addActionListener(e -> configurar());
        olvidar.addActionListener(e -> {
            CuentaAiven.olvidarToken(preferencias);
            actualizar();
        });
        actualizar();
    }

    @Override
    public void removeNotify() {
        // Cerrar la ventana corta el sondeo: no queda un Timer pidiendo a Render en segundo plano.
        cerrado = true;
        if (proximo != null) proximo.stop();
        super.removeNotify();
    }

    private CuentaAiven cuenta() {
        CuentaAiven cuenta = CuentaAiven.leer(preferencias, sistema);
        consola.setVisible(!cuenta.conToken());
        olvidar.setVisible(cuenta.conToken() && !cuenta.tokenDelSistema());
        return cuenta;
    }

    private ApiAiven apiAiven(CuentaAiven cuenta) {
        return new ApiAiven(urlAiven, cuenta.token(), cuenta.proyecto(), cuenta.servicio());
    }

    private void actualizar() {
        avisar(" ", null);
        consultarAiven(cuenta(), estado -> { }, error -> { });
        mostrar(render, new EstadoServicio(Fase.DESCONOCIDO, "consultando…"));
        Tarea.ejecutar(this, () -> new ApiRender(urlRender).estado(ESPERA_RENDER), e -> mostrar(render, e),
                error -> mostrar(render, EstadoServicio.error(error.getMessage())));
    }

    private void consultarAiven(CuentaAiven cuenta, Consumer<EstadoServicio> siguiente, Consumer<ErrorApi> alFallar) {
        if (!cuenta.completa()) {
            EstadoServicio desconocido = new EstadoServicio(Fase.DESCONOCIDO, cuenta.conToken()
                    ? "falta el proyecto o el servicio (Configurar Aiven…)" : "sin token de Aiven");
            mostrar(aiven, desconocido);
            siguiente.accept(desconocido);
            return;
        }
        Tarea.ejecutar(this, () -> apiAiven(cuenta).estado(), estado -> {
            mostrar(aiven, estado);
            siguiente.accept(estado);
        }, error -> {
            mostrar(aiven, EstadoServicio.error(error.getMessage()));
            alFallar.accept(error);
        });
    }

    private void despertar() {
        despertar.setEnabled(false);
        actualizar.setEnabled(false);
        avisar("Despertando producción: puede tardar varios minutos", Colores.aviso());
        CuentaAiven cuenta = cuenta();
        consultarAiven(cuenta, estado -> {
            if (!cuenta.completa()) {
                esperarRender();
            } else if (estado.fase() == Fase.APAGADO) {
                mostrar(aiven, EstadoServicio.de(Fase.ENCENDIENDO));
                Tarea.ejecutar(this, () -> {
                    apiAiven(cuenta).encender();
                    return null;
                }, nada -> esperar(() -> apiAiven(cuenta).estado(), aiven, this::esperarRender), this::fallo);
            } else if (estado.enLinea()) {
                esperarRender();
            } else {
                esperar(() -> apiAiven(cuenta).estado(), aiven, this::esperarRender);
            }
        }, this::fallo);
    }

    private void esperarRender() {
        esperar(() -> new ApiRender(urlRender).estado(ESPERA_RENDER), render, () -> {
            avisar("Producción en línea", Colores.exito());
            terminar();
        });
    }

    private void esperar(Callable<EstadoServicio> pedir, JLabel donde, Runnable alEstarEnLinea) {
        limite = Instant.now().plus(PLAZO);
        sondear(pedir, donde, alEstarEnLinea);
    }

    private void sondear(Callable<EstadoServicio> pedir, JLabel donde, Runnable alEstarEnLinea) {
        if (cerrado) return;
        Tarea.ejecutar(this, pedir, estado -> {
            mostrar(donde, estado);
            if (estado.enLinea()) {
                alEstarEnLinea.run();
            } else if (Instant.now().isAfter(limite)) {
                avisar("No terminó de encender en " + PLAZO.toMinutes() + " minutos: probá de nuevo o mirá la consola",
                        Colores.error());
                terminar();
            } else {
                proximo = new Timer((int) intervalo.toMillis(), e -> sondear(pedir, donde, alEstarEnLinea));
                proximo.setRepeats(false);
                proximo.start();
            }
        }, error -> {
            mostrar(donde, EstadoServicio.error(error.getMessage()));
            fallo(error);
        });
    }

    private void fallo(ErrorApi error) {
        avisar(error.getMessage(), Colores.error());
        terminar();
    }

    private void terminar() {
        despertar.setEnabled(true);
        actualizar.setEnabled(true);
    }

    private void mostrar(JLabel donde, EstadoServicio estado) {
        donde.setText(estado.texto());
        donde.setForeground(switch (estado.fase()) {
            case EN_LINEA -> Colores.exito();
            case ENCENDIENDO -> Colores.aviso();
            case ERROR -> Colores.error();
            case APAGADO, DESCONOCIDO -> Colores.secundario();
        });
    }

    private void avisar(String texto, Color color) {
        mensaje.setText(texto);
        if (color != null) mensaje.setForeground(color);
    }

    private void abrirConsola() {
        try {
            Desktop.getDesktop().browse(URI.create(ApiAiven.CONSOLA));
        } catch (IOException | UnsupportedOperationException e) {
            Mensajes.error(this, "No se pudo abrir el navegador: entrá a " + ApiAiven.CONSOLA);
        }
    }

    private void configurar() {
        CuentaAiven actual = cuenta();
        JTextField proyecto = new JTextField(actual.proyecto() == null ? "" : actual.proyecto(), 24);
        JTextField servicio = new JTextField(actual.servicio() == null ? "" : actual.servicio(), 24);
        JPasswordField token = new JPasswordField(24);
        token.setEnabled(!actual.tokenDelSistema());
        Formulario formulario = new Formulario()
                .campo("Proyecto", proyecto)
                .campo("Servicio", servicio)
                .campo("Token", token)
                .ancho(Componentes.nota(actual.tokenDelSistema() ? "El token sale de AIVEN_TOKEN"
                        : actual.conToken() ? "Hay un token guardado: dejalo vacío para conservarlo"
                        : "Se guarda en esta compu, no en el repo"));
        int respuesta = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), formulario,
                "Cuenta de Aiven", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) return;
        CuentaAiven.guardar(preferencias, proyecto.getText(), servicio.getText(), new String(token.getPassword()));
        actualizar();
    }
}
