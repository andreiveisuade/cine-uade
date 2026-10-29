package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.CampoImagen;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Seccion;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.horaDelDia;

// El alta y la edición de una promoción (CU-17): una tarjeta de beneficio por tipo, con los campos del catálogo.
// Al editar el tipo queda fijo: el backend no deja cambiarlo.
final class FormularioPromocion extends Seccion {

    private final ApiPromociones apiPromociones;
    private final Runnable alGuardar;
    private final JLabel titulo = Componentes.subtitulo("Nueva promoción");
    private final JTextField nombre = new JTextField();
    private final JComboBox<Opcion<String>> tipo = new JComboBox<>();
    private final TarjetasBeneficio beneficio = new TarjetasBeneficio();
    private final CampoFecha desde = new CampoFecha(LocalDate.now());
    private final CampoFecha hasta = new CampoFecha(null);
    private final SelectorDias dias = new SelectorDias();
    private final JTextField horaDesde = new JTextField();
    private final JTextField horaHasta = new JTextField();
    private final Map<String, JCheckBox> medios = new LinkedHashMap<>();
    private final JPanel panelMedios = new JPanel(new GridLayout(0, 2, 4, 0));
    private final CampoImagen imagen;
    private final JButton guardar = Componentes.primario(new JButton("Crear promoción"));
    private final JButton cancelar = new JButton("Cancelar");
    private final JLabel error = Componentes.texto(" ");
    private Promocion editando;

    FormularioPromocion(ApiPromociones apiPromociones, ApiImagenes apiImagenes, Runnable alGuardar) {
        super(new BorderLayout());
        this.apiPromociones = apiPromociones;
        this.alGuardar = alGuardar;
        this.imagen = new CampoImagen(apiImagenes);
        tipo.addActionListener(e -> {
            if (Campos.elegido(tipo) != null) beneficio.mostrar(Campos.elegido(tipo));
        });
        horaDesde.setToolTipText("HH:mm, vacío = sin límite");
        horaHasta.setToolTipText("HH:mm, vacío = sin límite");
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> limpiar());
        cancelar.setVisible(false);

        add(Componentes.lateral(Componentes.conBorde(new Formulario()
                .ancho(titulo)
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
                // R15: la web cobra con Mercado Pago, sin promo por medio; por eso no figura entre estos.
                .ancho(Componentes.nota("Los medios aplican en boletería"))
                .campo("Imagen", imagen)
                .ancho(Componentes.aLaDerecha(cancelar, guardar))
                .ancho(error)
                .ancho(Componentes.nota("Las condiciones se evalúan contra el horario de la <b>función</b>, no "
                        + "contra el momento de la compra: un 2x1 de los miércoles vale para la función del miércoles "
                        + "aunque las entradas se compren el lunes."))
                .cerrar())));
    }

    /** Una tarjeta por tipo, con los campos que pide. Cambiar de tipo muestra la suya. */
    void armarTipos(List<TipoPromocion> tipos) {
        for (TipoPromocion t : tipos) {
            beneficio.agregar(t);
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

    /** Precarga todo lo de la promoción; guardar la edita en vez de crear otra. */
    void editar(Promocion p) {
        editando = p;
        titulo.setText("Editar " + p.nombre());
        nombre.setText(p.nombre());
        nombre.setCaretPosition(0);
        Campos.elegir(tipo, p.tipo());
        tipo.setEnabled(false);
        beneficio.poner(p);
        desde.poner(LocalDate.parse(p.vigenciaDesde()));
        hasta.poner(LocalDate.parse(p.vigenciaHasta()));
        dias.poner(p.diasSemana());
        horaDesde.setText(hora(p.horaDesde()));
        horaHasta.setText(hora(p.horaHasta()));
        medios.forEach((medio, caja) -> caja.setSelected(p.mediosPago() != null && p.mediosPago().contains(medio)));
        imagen.poner(p.imagenUrl());
        guardar.setText("Guardar cambios");
        cancelar.setVisible(true);
        error.setText(" ");
    }

    private void guardar() {
        Validacion v = new Validacion(error);
        String nombreLeido = v.texto(nombre, "Nombre", true);
        String elegido = v.elegido(tipo, "Tipo");
        Map<CampoBeneficio, Number> leidos = beneficio.leer(v, elegido);
        String vigenciaDesde = v.fecha(desde, "Desde", true);
        String vigenciaHasta = v.fecha(hasta, "Hasta", true);
        String desdeHora = v.hora(horaDesde, "Desde hora", false);
        String hastaHora = v.hora(horaHasta, "Hasta hora", false);
        v.alMencionar("vigencia", desde);
        beneficio.caja(elegido, CampoBeneficio.LLEVA).ifPresent(lleva -> v.alMencionar("nxm", lleva));
        if (!v.ok()) return;
        PedidoPromocion pedido = new PedidoPromocion(nombreLeido, elegido,
                (Double) leidos.get(CampoBeneficio.PORCENTAJE), (Double) leidos.get(CampoBeneficio.MONTO),
                (Integer) leidos.get(CampoBeneficio.LLEVA), (Integer) leidos.get(CampoBeneficio.PAGA),
                vigenciaDesde, vigenciaHasta, dias.elegidos(), desdeHora, hastaHora, tildados(), imagen.url());
        Promocion actual = editando;
        Tarea.ejecutar(this, () -> actual == null ? apiPromociones.crearPromocion(pedido)
                : apiPromociones.editarPromocion(actual.id(), pedido), guardada -> {
            avisar(actual == null ? "Promoción creada" : "Cambios guardados");
            limpiar();
            alGuardar.run();
        }, v::mostrarError);
    }

    private List<String> tildados() {
        return medios.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
    }

    private static String hora(String iso) {
        return iso == null ? "" : horaDelDia(iso);
    }

    private void limpiar() {
        editando = null;
        titulo.setText("Nueva promoción");
        tipo.setEnabled(true);
        nombre.setText("");
        if (tipo.getItemCount() > 0) tipo.setSelectedIndex(0);
        beneficio.reiniciar();
        desde.poner(LocalDate.now());
        hasta.poner(null);
        dias.limpiar();
        horaDesde.setText("");
        horaHasta.setText("");
        medios.values().forEach(c -> c.setSelected(false));
        imagen.poner(null);
        guardar.setText("Crear promoción");
        cancelar.setVisible(false);
        error.setText(" ");
    }
}
