package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.SelectorDias;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.horaDelDia;
import static ar.uade.cine.swing.comun.Formato.precio;

// Promociones (CU-17): el listado con su baja y reactivación, y el alta al costado (FormularioPromocion).
/** No se borran: una que ya se usó explica por qué se cobró ese monto. */
public final class PantallaPromociones extends Pantalla {

    private record Datos(List<Promocion> promociones, List<MedioPago> medios) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiPromociones apiPromociones;
    private final FormularioPromocion formulario;
    private final JLabel resumen = new JLabel(" ");
    private final JButton alternar = new JButton("Dar de baja");
    private final Tabla<Promocion> tabla = new Tabla<>(
            Columna.<Promocion>de("Nombre", Promocion::nombre).ancho(180),
            Columna.<Promocion>de("Beneficio", PantallaPromociones::beneficio),
            Columna.<Promocion>de("Vigencia", p -> p.vigenciaDesde() + " al " + p.vigenciaHasta()).ancho(170),
            Columna.<Promocion>de("Cuándo", PantallaPromociones::condiciones).ancho(240),
            Columna.<Promocion>de("Estado", p -> p.activa() ? "Activa" : "Dada de baja"));

    public PantallaPromociones(ApiCatalogos apiCatalogos, ApiPromociones apiPromociones) {
        super("Promociones", "No se acumulan: en cada cobro se aplica la que más descuenta.");
        this.apiCatalogos = apiCatalogos;
        this.apiPromociones = apiPromociones;
        this.formulario = new FormularioPromocion(apiPromociones, this::recargar);

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
        add(formulario, BorderLayout.EAST);

        tabla.tabla().getSelectionModel().addListSelectionListener(e -> habilitar());
        alternar.addActionListener(e -> tabla.seleccionada().ifPresent(p -> {
            if (p.activa() && !confirmar("¿Dar de baja " + p.nombre() + "? Deja de aplicarse en los cobros.",
                    "Sí, dar de baja")) return;
            accion(() -> apiPromociones.cambiarActivacionPromocion(p.id(), !p.activa()),
                    p.nombre() + (p.activa() ? " dada de baja" : " reactivada"), this::recargar);
        }));
        habilitar();
        cargar(apiCatalogos::obtenerTiposPromocion, formulario::armarTipos);
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

    private void recargar() {
        cargar(() -> new Datos(apiPromociones.obtenerPromociones(), apiCatalogos.obtenerMediosPago()), datos -> {
            long activas = datos.promociones().stream().filter(Promocion::activa).count();
            resumen.setText(activas + " activas de " + datos.promociones().size());
            tabla.mostrar(datos.promociones());
            formulario.llenarMedios(datos.medios());
            habilitar();
        });
    }
}
