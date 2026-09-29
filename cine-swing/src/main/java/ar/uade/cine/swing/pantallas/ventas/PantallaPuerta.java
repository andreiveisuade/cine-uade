package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.Tarifa;
import ar.uade.cine.swing.api.dto.ventas.Entrada;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Set;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.escapar;
import static ar.uade.cine.swing.comun.Formato.fechaHora;

/** Control de acceso (CU-18). Lo único que ve el acomodador. */
public final class PantallaPuerta extends Pantalla {

    private record Validada(Reserva reserva, Set<String> seAcreditan) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiVentas apiVentas;
    private final JTextField codigo = new JTextField(10);
    private final JLabel resultado = new JLabel();
    private final JLabel error = Componentes.texto(" ");
    // Las tarifas que piden carnet, del catálogo: se piden una vez, en el primer código.
    private volatile Set<String> seAcreditan;

    public PantallaPuerta(ApiCatalogos apiCatalogos, ApiVentas apiVentas) {
        super("Validar entrada", "Escaneá el código del ticket o tipealo. Cada entrada sirve una sola vez.");
        this.apiCatalogos = apiCatalogos;
        this.apiVentas = apiVentas;

        codigo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        JButton validar = new JButton("Validar");
        validar.setFont(validar.getFont().deriveFont(18f));
        JPanel fila = new JPanel(new FlujoConSalto());
        fila.add(codigo);
        fila.add(validar);
        resultado.setVerticalAlignment(JLabel.TOP);

        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(fila, BorderLayout.NORTH);
        arriba.add(error, BorderLayout.SOUTH);
        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(resultado, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        codigo.addActionListener(e -> validar());
        validar.addActionListener(e -> validar());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        codigo.requestFocusInWindow();
    }

    // El foco vuelve al campo tras cada validación: en la puerta se encadenan una atrás de otra.
    private void validar() {
        Validacion v = new Validacion(error);
        String tipeado = v.texto(codigo, "Código", true);
        if (!v.ok()) return;
        String limpio = tipeado.toUpperCase();
        // El catálogo antes que el acceso: si fallara después, la entrada quedaría usada y en pantalla diría NO PASA.
        Tarea.ejecutar(this, () -> {
            Set<String> tarifas = tarifasQueSeAcreditan();
            return new Validada(apiVentas.validarEntrada(limpio), tarifas);
        }, validada -> {
            mostrarValida(validada.reserva(), validada.seAcreditan());
            reiniciar();
        }, error -> {
            // Los tres motivos se muestran igual de fuerte: en la puerta solo importa que no pasa. Pero sin conexión
            // o con el servidor caído no se sabe si pasa: eso no es un NO PASA, es un error global.
            if (error.esSesionVencida()) return;
            if (!error.esDelFormulario()) {
                Mensajes.error(this, error);
                reiniciar();
                return;
            }
            mostrar(Colores.error(), "NO PASA", "<p>" + escapar(error.getMessage()) + "</p>");
            reiniciar();
        });
    }

    private void reiniciar() {
        codigo.setText("");
        codigo.requestFocusInWindow();
    }

    private Set<String> tarifasQueSeAcreditan() {
        if (seAcreditan == null) {
            seAcreditan = apiCatalogos.obtenerTarifas().stream().filter(Tarifa::requiereAcreditacion)
                    .map(Tarifa::nombre).collect(Collectors.toSet());
        }
        return seAcreditan;
    }

    private void mostrarValida(Reserva reserva, Set<String> seAcreditan) {
        StringBuilder html = new StringBuilder();
        html.append("<p><b>").append(reserva.pelicula() == null ? "" : escapar(reserva.pelicula().titulo()))
                .append("</b><br>").append(reserva.sala() == null ? "" : escapar(reserva.sala().nombre()))
                .append(" · ").append(reserva.funcion() == null ? "" : fechaHora(reserva.funcion().inicio()))
                .append("</p><table>");
        for (Entrada e : reserva.entradas()) {
            String tarifa = e.tarifa();
            boolean pideCarnet = seAcreditan.contains(tarifa);
            html.append("<tr><td><tt><b>").append(e.codigo()).append("</b></tt></td><td>")
                    .append(pideCarnet ? "<b>" + etiqueta(tarifa) + " · pedir carnet</b>" : etiqueta(tarifa))
                    .append("</td></tr>");
        }
        int personas = reserva.entradas().size();
        html.append("</table><p>").append(cantidad(personas, "persona", "personas"))
                .append(" · ingreso registrado ").append(fechaHora(reserva.ingresadaEn())).append("</p>");
        mostrar(Colores.exito(), "ADELANTE", html.toString());
    }

    private void mostrar(Color color, String titulo, String cuerpo) {
        resultado.setText("<html><div style='width:460px'><h1 style='color:" + Colores.hex(color) + "'>" + titulo
                + "</h1>" + cuerpo + "</div></html>");
        resultado.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }
}
