package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.horaDelDia;
import static ar.uade.cine.swing.comun.Formato.precio;

/** Alta y baja de promociones (CU-17). No se borran: una que ya se usó explica por qué se cobró ese monto. */
final class PantallaPromociones extends Pantalla {

    private record Datos(List<Promocion> promociones, List<MedioPago> medios) {
    }

    /**
     * Lo que Swing sabe de cada campo de beneficio: cómo se muestra, si es entero y con qué arranca. Cuáles pide
     * cada tipo lo dice {@code GET /api/tipos-promocion}: un tipo nuevo que use estos campos no toca esta pantalla.
     */
    private record CampoBeneficio(String etiqueta, boolean entero, String inicial) {
    }

    private static final Map<String, CampoBeneficio> CAMPOS = Map.of(
            "porcentaje", new CampoBeneficio("Porcentaje", false, "30"),
            "monto", new CampoBeneficio("Monto a descontar", false, "2000"),
            "lleva", new CampoBeneficio("Lleva", true, "2"),
            "paga", new CampoBeneficio("Paga", true, "1"));

    private final ApiCatalogos apiCatalogos;
    private final ApiPromociones apiPromociones;
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
    // Tipo → campo → su caja de texto. Cada tipo tiene las suyas: una caja no puede estar en dos tarjetas.
    private final Map<String, Map<String, JTextField>> camposPorTipo = new LinkedHashMap<>();
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JDateChooser hasta = Fechas.selector(null);
    private final SelectorDias dias = new SelectorDias();
    private final JTextField horaDesde = new JTextField();
    private final JTextField horaHasta = new JTextField();
    private final Map<String, JCheckBox> medios = new LinkedHashMap<>();
    private final JPanel panelMedios = new JPanel(new GridLayout(0, 2, 4, 0));
    private final JLabel error = Componentes.texto(" ");

    PantallaPromociones(ApiCatalogos apiCatalogos, ApiPromociones apiPromociones) {
        super("Promociones", "No se acumulan: en cada cobro se aplica la que más descuenta.");
        this.apiCatalogos = apiCatalogos;
        this.apiPromociones = apiPromociones;

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
        alternar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (p.activa() && !confirmar("¿Dar de baja " + p.nombre() + "? Deja de aplicarse en los cobros.",
                    "Sí, dar de baja")) return;
            accion(() -> apiPromociones.cambiarActivacionPromocion(p.id(), !p.activa()),
                    p.nombre() + (p.activa() ? " dada de baja" : " reactivada"), this::recargar);
        }));
        habilitar();
        cargar(apiCatalogos::obtenerTiposPromocion, this::armarTipos);
        recargar();
    }

    // Por los campos que trae, no por el nombre del tipo: son los mismos que el catálogo dice que pide cada uno.
    static String beneficio(Promocion p) {
        if (p.porcentaje() != null) {
            return (p.porcentaje() % 1 == 0 ? String.valueOf(p.porcentaje().longValue())
                    : String.valueOf(p.porcentaje())) + "% off";
        }
        if (p.monto() != null) return precio(p.monto()) + " off";
        return p.lleva() + "x" + p.paga();
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
        return hora == null ? siFalta : horaDelDia(hora);
    }

    private void habilitar() {
        var elegida = tabla.seleccionada();
        alternar.setEnabled(elegida.isPresent());
        alternar.setText(elegida.map(p -> p.activa() ? "Dar de baja" : "Reactivar").orElse("Dar de baja"));
    }

    private JScrollPane formulario() {
        tipo.addActionListener(e -> {
            if (Campos.elegido(tipo) != null) tarjetas.show(beneficio, Campos.elegido(tipo));
        });

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
        return Componentes.lateral(Componentes.conBorde(formulario));
    }

    /** Una tarjeta por tipo, con los campos que pide. Cambiar de tipo muestra la suya. */
    private void armarTipos(List<TipoPromocion> tipos) {
        for (TipoPromocion t : tipos) {
            Map<String, JTextField> cajas = new LinkedHashMap<>();
            Componentes.Formulario tarjeta = new Componentes.Formulario();
            for (String campo : t.campos()) {
                CampoBeneficio c = CAMPOS.get(campo);
                if (c == null) continue;
                JTextField caja = new JTextField(c.inicial());
                cajas.put(campo, c.entero() ? Campos.soloEntero(caja) : Campos.soloDecimal(caja));
                tarjeta.obligatorio(c.etiqueta(), caja);
            }
            camposPorTipo.put(t.nombre(), cajas);
            beneficio.add(tarjeta, t.nombre());
            tipo.addItem(new Opcion<>(t.nombre(), etiqueta(t.nombre())));
        }
        beneficio.revalidate();
    }

    private void recargar() {
        cargar(() -> new Datos(apiPromociones.obtenerPromociones(), apiCatalogos.obtenerMediosPago()), datos -> {
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
        Map<String, JTextField> cajas = camposPorTipo.getOrDefault(elegido, Map.of());
        Map<String, Number> leidos = new HashMap<>();
        cajas.forEach((campo, caja) -> {
            CampoBeneficio c = CAMPOS.get(campo);
            leidos.put(campo, c.entero() ? v.entero(caja, c.etiqueta(), true) : v.decimal(caja, c.etiqueta(), true));
        });
        String vigenciaDesde = v.fecha(desde, "Desde", true);
        String vigenciaHasta = v.fecha(hasta, "Hasta", true);
        String desdeHora = v.hora(horaDesde, "Desde hora", false);
        String hastaHora = v.hora(horaHasta, "Hasta hora", false);
        v.alMencionar("vigencia", desde);
        if (cajas.containsKey("lleva")) v.alMencionar("nxm", cajas.get("lleva"));
        if (!v.ok()) return;
        PedidoPromocion pedido = new PedidoPromocion(nombreLeido, elegido, (Double) leidos.get("porcentaje"),
                (Double) leidos.get("monto"), (Integer) leidos.get("lleva"), (Integer) leidos.get("paga"),
                vigenciaDesde, vigenciaHasta, dias.elegidos(), desdeHora, hastaHora, tildados(medios));
        Tarea.ejecutar(this, () -> apiPromociones.crearPromocion(pedido), creada -> {
            avisar("Promoción creada");
            limpiar();
            recargar();
        }, v::mostrarError);
    }

    private void limpiar() {
        nombre.setText("");
        if (tipo.getItemCount() > 0) tipo.setSelectedIndex(0);
        camposPorTipo.values().forEach(cajas -> cajas.forEach((campo, caja) ->
                caja.setText(CAMPOS.get(campo).inicial())));
        Fechas.poner(desde, LocalDate.now());
        hasta.setDate(null);
        dias.limpiar();
        horaDesde.setText("");
        horaHasta.setText("");
        medios.values().forEach(c -> c.setSelected(false));
    }
}
