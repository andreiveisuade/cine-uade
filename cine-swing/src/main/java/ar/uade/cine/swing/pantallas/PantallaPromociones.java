package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.MedioPago;
import ar.uade.cine.swing.api.dto.PedidoPromocion;
import ar.uade.cine.swing.api.dto.Promocion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.precio;

/** Alta y baja de promociones (CU-17). No se borran: una que ya se usó explica por qué se cobró ese monto. */
final class PantallaPromociones extends Pantalla {

    private record Datos(List<Promocion> promociones, List<MedioPago> medios) {
    }

    private final JLabel resumen = new JLabel(" ");
    private final JButton alternar = new JButton("Dar de baja");
    private final Tabla<Promocion> tabla = new Tabla<>(
            Columna.<Promocion>de("Nombre", Promocion::nombre).ancho(180),
            Columna.<Promocion>de("Beneficio", PantallaPromociones::beneficio),
            Columna.<Promocion>de("Vigencia", p -> p.vigenciaDesde() + " al " + p.vigenciaHasta()).ancho(170),
            Columna.<Promocion>de("Cuándo", PantallaPromociones::condiciones).ancho(240),
            Columna.<Promocion>de("Estado", p -> p.activa() ? "Activa" : "Dada de baja"));

    private final JTextField nombre = new JTextField();
    private final JComboBox<Opcion<String>> tipo = new JComboBox<>();
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel beneficio = new JPanel(tarjetas);
    private final JTextField porcentaje = Campos.soloDecimal(new JTextField("30"));
    private final JTextField monto = Campos.soloDecimal(new JTextField("2000"));
    private final JTextField lleva = Campos.soloEntero(new JTextField("2"));
    private final JTextField paga = Campos.soloEntero(new JTextField("1"));
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JDateChooser hasta = Fechas.selector(null);
    private final SelectorDias dias = new SelectorDias();
    private final JTextField horaDesde = new JTextField();
    private final JTextField horaHasta = new JTextField();
    private final Map<String, JCheckBox> medios = new LinkedHashMap<>();
    private final JPanel panelMedios = new JPanel(new GridLayout(0, 3, 4, 0));
    private final JLabel error = new JLabel(" ");

    PantallaPromociones(ApiHttp api) {
        super(api, "Promociones", "No se acumulan: en cada cobro se aplica la que más descuenta.");

        JPanel acciones = new JPanel(new FlujoConSalto());
        acciones.add(alternar);
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(acciones, BorderLayout.NORTH);
        abajo.add(Componentes.nota("Las promociones no se borran: se dan de baja. Una que ya se usó en un cobro "
                + "tiene que seguir existiendo para poder explicar por qué se cobró ese monto."), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(resumen, BorderLayout.NORTH);
        centro.add(tabla.conScroll(), BorderLayout.CENTER);
        centro.add(abajo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        add(formulario(), BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        alternar.addActionListener(e -> tabla.seleccionada().ifPresent(p ->
                accion(() -> api.cambiarActivacionPromocion(p.id(), !p.activa()), null, this::recargar)));
        habilitar();
        recargar();
    }

    static String beneficio(Promocion p) {
        return switch (p.tipo()) {
            case "PORCENTAJE" -> (p.porcentaje() % 1 == 0 ? String.valueOf(p.porcentaje().longValue())
                    : String.valueOf(p.porcentaje())) + "% off";
            case "MONTO_FIJO" -> precio(p.monto()) + " off";
            default -> p.lleva() + "x" + p.paga();
        };
    }

    static String condiciones(Promocion p) {
        List<String> partes = new ArrayList<>();
        if (p.diasSemana() != null && !p.diasSemana().isEmpty()) {
            partes.add(SelectorDias.resumen(p.diasSemana()));
        }
        if (p.horaDesde() != null || p.horaHasta() != null) {
            partes.add(corta(p.horaDesde(), "00:00") + "–" + corta(p.horaHasta(), "23:59"));
        }
        if (p.mediosPago() != null && !p.mediosPago().isEmpty()) {
            partes.add(p.mediosPago().stream().map(m -> etiqueta(m)).collect(Collectors.joining(", ")));
        }
        // Sin condiciones no quiere decir "ninguna": quiere decir que corre siempre.
        return partes.isEmpty() ? "todos los días, cualquier medio" : String.join(" · ", partes);
    }

    private static String corta(String hora, String siFalta) {
        return hora == null ? siFalta : hora.substring(0, Math.min(5, hora.length()));
    }

    private void habilitar() {
        var elegida = tabla.seleccionada();
        alternar.setEnabled(elegida.isPresent());
        alternar.setText(elegida.map(p -> p.activa() ? "Dar de baja" : "Reactivar").orElse("Dar de baja"));
    }

    private JPanel formulario() {
        tipo.addItem(new Opcion<>("PORCENTAJE", "Porcentaje"));
        tipo.addItem(new Opcion<>("MONTO_FIJO", "Monto fijo"));
        tipo.addItem(new Opcion<>("NXM", "NxM (2x1)"));
        beneficio.add(new Componentes.Formulario().obligatorio("Porcentaje", porcentaje), "PORCENTAJE");
        beneficio.add(new Componentes.Formulario().obligatorio("Monto a descontar", monto), "MONTO_FIJO");
        beneficio.add(new Componentes.Formulario().obligatorio("Lleva", lleva).obligatorio("Paga", paga), "NXM");
        tipo.addActionListener(e -> tarjetas.show(beneficio, Campos.elegido(tipo)));

        horaDesde.setToolTipText("HH:mm, vacío = sin límite");
        horaHasta.setToolTipText("HH:mm, vacío = sin límite");

        JButton crear = new JButton("Crear promoción");
        crear.addActionListener(e -> crear());

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(Componentes.subtitulo("Nueva promoción"))
                .obligatorio("Nombre", nombre)
                .obligatorio("Tipo", tipo)
                .ancho(beneficio)
                .obligatorio("Desde", desde)
                .obligatorio("Hasta", hasta)
                .ancho(new JLabel("Días (ninguno = todos)"))
                .ancho(dias)
                .campo("Desde hora", horaDesde)
                .campo("Hasta hora", horaHasta)
                .ancho(new JLabel("Medios (ninguno = cualquiera)"))
                .ancho(panelMedios)
                .ancho(crear)
                .ancho(error)
                .ancho(Componentes.nota("Las condiciones se evalúan contra el horario de la <b>función</b>, no "
                        + "contra el momento de la compra: un 2x1 de los miércoles vale para la función del miércoles "
                        + "aunque las entradas se compren el lunes."))
                .cerrar();
        JPanel panel = Componentes.conBorde(formulario);
        panel.setPreferredSize(new Dimension(380, 0));
        return panel;
    }

    private void recargar() {
        cargar(() -> new Datos(api.obtenerPromociones(), api.obtenerMediosPago()), datos -> {
            long activas = datos.promociones().stream().filter(Promocion::activa).count();
            resumen.setText(activas + " activas de " + datos.promociones().size());
            tabla.mostrar(datos.promociones());
            if (medios.isEmpty()) {
                for (MedioPago m : datos.medios()) {
                    JCheckBox caja = new JCheckBox(etiqueta(m.nombre()));
                    medios.put(m.nombre(), caja);
                    panelMedios.add(caja);
                }
                panelMedios.revalidate();
            }
            habilitar();
        });
    }

    private static List<String> tildados(Map<String, JCheckBox> cajas) {
        return cajas.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
    }

    private void crear() {
        Validacion v = new Validacion(error);
        String nombreLeido = v.texto(nombre, "Nombre", true);
        String elegido = v.elegido(tipo, "Tipo");
        // Solo se leen los campos del tipo elegido: los demás viajan en null, como pide el contrato.
        Double porcentajeLeido = "PORCENTAJE".equals(elegido) ? v.decimal(porcentaje, "Porcentaje", true) : null;
        Double montoLeido = "MONTO_FIJO".equals(elegido) ? v.decimal(monto, "Monto a descontar", true) : null;
        Integer llevaLeido = "NXM".equals(elegido) ? v.entero(lleva, "Lleva", true) : null;
        Integer pagaLeido = "NXM".equals(elegido) ? v.entero(paga, "Paga", true) : null;
        String vigenciaDesde = v.fecha(desde, "Desde", true);
        String vigenciaHasta = v.fecha(hasta, "Hasta", true);
        String desdeHora = v.hora(horaDesde, "Desde hora", false);
        String hastaHora = v.hora(horaHasta, "Hasta hora", false);
        v.alMencionar("vigencia", desde);
        v.alMencionar("nxm", lleva);
        if (!v.ok()) return;
        PedidoPromocion pedido = new PedidoPromocion(nombreLeido, elegido, porcentajeLeido, montoLeido, llevaLeido,
                pagaLeido, vigenciaDesde, vigenciaHasta, dias.elegidos(), desdeHora, hastaHora, tildados(medios));
        Tarea.ejecutar(this, () -> api.crearPromocion(pedido), creada -> {
            avisar("Promoción creada");
            limpiar();
            recargar();
        }, v::mostrarError);
    }

    private void limpiar() {
        nombre.setText("");
        tipo.setSelectedIndex(0);
        porcentaje.setText("30");
        monto.setText("2000");
        lleva.setText("2");
        paga.setText("1");
        Fechas.poner(desde, LocalDate.now());
        hasta.setDate(null);
        dias.limpiar();
        horaDesde.setText("");
        horaHasta.setText("");
        medios.values().forEach(c -> c.setSelected(false));
    }
}
