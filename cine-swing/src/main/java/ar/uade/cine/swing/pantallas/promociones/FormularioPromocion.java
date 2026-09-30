package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// El alta de una promoción (CU-17): una tarjeta de beneficio por tipo, con los campos que pide el catálogo.
final class FormularioPromocion extends Seccion {

    private final ApiPromociones apiPromociones;
    private final Runnable alCrear;
    private final JTextField nombre = new JTextField();
    private final JComboBox<Opcion<String>> tipo = new JComboBox<>();
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel beneficio = new JPanel(tarjetas);
    // Tipo → campo → su caja de texto. Cada tipo tiene las suyas: una caja no puede estar en dos tarjetas.
    private final Map<String, Map<CampoBeneficio, JTextField>> camposPorTipo = new LinkedHashMap<>();
    private final JDateChooser desde = Fechas.selector(LocalDate.now());
    private final JDateChooser hasta = Fechas.selector(null);
    private final SelectorDias dias = new SelectorDias();
    private final JTextField horaDesde = new JTextField();
    private final JTextField horaHasta = new JTextField();
    private final Map<String, JCheckBox> medios = new LinkedHashMap<>();
    private final JPanel panelMedios = new JPanel(new GridLayout(0, 2, 4, 0));
    private final JLabel error = Componentes.texto(" ");

    FormularioPromocion(ApiPromociones apiPromociones, Runnable alCrear) {
        super(new BorderLayout());
        this.apiPromociones = apiPromociones;
        this.alCrear = alCrear;
        tipo.addActionListener(e -> {
            if (Campos.elegido(tipo) != null) tarjetas.show(beneficio, Campos.elegido(tipo));
        });
        horaDesde.setToolTipText("HH:mm, vacío = sin límite");
        horaHasta.setToolTipText("HH:mm, vacío = sin límite");
        JButton crear = new JButton("Crear promoción");
        crear.addActionListener(e -> crear());

        add(Componentes.lateral(Componentes.conBorde(new Formulario()
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
                .cerrar())));
    }

    /** Una tarjeta por tipo, con los campos que pide. Cambiar de tipo muestra la suya. */
    void armarTipos(List<TipoPromocion> tipos) {
        for (TipoPromocion t : tipos) {
            Map<CampoBeneficio, JTextField> cajas = new LinkedHashMap<>();
            Formulario tarjeta = new Formulario();
            t.campos().stream().flatMap(campo -> CampoBeneficio.llamado(campo).stream()).forEach(campo -> {
                JTextField caja = campo.caja();
                cajas.put(campo, caja);
                tarjeta.obligatorio(campo.etiqueta(), caja);
            });
            camposPorTipo.put(t.nombre(), cajas);
            beneficio.add(tarjeta, t.nombre());
            tipo.addItem(new Opcion<>(t.nombre(), etiqueta(t.nombre())));
        }
        beneficio.revalidate();
    }

    /** Las casillas de medios de pago se arman una sola vez: recargar el listado no las repite. */
    void llenarMedios(List<MedioPago> catalogo) {
        if (!medios.isEmpty()) return;
        for (MedioPago m : catalogo) {
            JCheckBox caja = new JCheckBox(etiqueta(m.nombre()));
            medios.put(m.nombre(), caja);
            panelMedios.add(caja);
        }
        panelMedios.revalidate();
    }

    private void crear() {
        Validacion v = new Validacion(error);
        String nombreLeido = v.texto(nombre, "Nombre", true);
        String elegido = v.elegido(tipo, "Tipo");
        // Solo se leen los campos del tipo elegido: los demás viajan en null, como pide el contrato.
        Map<CampoBeneficio, JTextField> cajas = camposPorTipo.getOrDefault(elegido, Map.of());
        Map<String, Number> leidos = new HashMap<>();
        cajas.forEach((campo, caja) -> leidos.put(campo.nombre(), campo.leer(v, caja)));
        String vigenciaDesde = v.fecha(desde, "Desde", true);
        String vigenciaHasta = v.fecha(hasta, "Hasta", true);
        String desdeHora = v.hora(horaDesde, "Desde hora", false);
        String hastaHora = v.hora(horaHasta, "Hasta hora", false);
        v.alMencionar("vigencia", desde);
        if (cajas.containsKey(CampoBeneficio.LLEVA)) v.alMencionar("nxm", cajas.get(CampoBeneficio.LLEVA));
        if (!v.ok()) return;
        PedidoPromocion pedido = new PedidoPromocion(nombreLeido, elegido, (Double) leidos.get("porcentaje"),
                (Double) leidos.get("monto"), (Integer) leidos.get("lleva"), (Integer) leidos.get("paga"),
                vigenciaDesde, vigenciaHasta, dias.elegidos(), desdeHora, hastaHora, tildados());
        Tarea.ejecutar(this, () -> apiPromociones.crearPromocion(pedido), creada -> {
            avisar("Promoción creada");
            limpiar();
            alCrear.run();
        }, v::mostrarError);
    }

    private List<String> tildados() {
        return medios.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
    }

    private void limpiar() {
        nombre.setText("");
        if (tipo.getItemCount() > 0) tipo.setSelectedIndex(0);
        camposPorTipo.values().forEach(cajas -> cajas.forEach(CampoBeneficio::reiniciar));
        Fechas.poner(desde, LocalDate.now());
        hasta.setDate(null);
        dias.limpiar();
        horaDesde.setText("");
        horaHasta.setText("");
        medios.values().forEach(c -> c.setSelected(false));
    }
}
