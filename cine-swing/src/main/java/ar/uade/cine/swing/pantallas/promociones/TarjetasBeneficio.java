package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Validacion;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// El beneficio de una promoción: una tarjeta por tipo con los campos que pide, y se ve la del tipo elegido.
final class TarjetasBeneficio extends JPanel {

    private final CardLayout tarjetas = new CardLayout();
    // Tipo → campo → su caja de texto. Cada tipo tiene las suyas: una caja no puede estar en dos tarjetas.
    private final Map<String, Map<CampoBeneficio, JTextField>> camposPorTipo = new LinkedHashMap<>();

    TarjetasBeneficio() {
        setLayout(tarjetas);
    }

    void agregar(TipoPromocion t) {
        Map<CampoBeneficio, JTextField> cajas = new LinkedHashMap<>();
        Formulario tarjeta = new Formulario();
        t.campos().stream().flatMap(campo -> CampoBeneficio.llamado(campo).stream()).forEach(campo -> {
            JTextField caja = campo.caja();
            cajas.put(campo, caja);
            tarjeta.obligatorio(campo.etiqueta(), caja);
        });
        camposPorTipo.put(t.nombre(), cajas);
        add(tarjeta, t.nombre());
    }

    void mostrar(String tipo) {
        tarjetas.show(this, tipo);
    }

    void poner(Promocion p) {
        camposPorTipo.getOrDefault(p.tipo(), Map.of()).forEach((campo, caja) -> campo.poner(caja, p));
    }

    // Solo se leen los campos del tipo elegido: los demás viajan en null, como pide el contrato.
    Map<String, Number> leer(Validacion v, String tipo) {
        Map<String, Number> leidos = new HashMap<>();
        camposPorTipo.getOrDefault(tipo, Map.of())
                .forEach((campo, caja) -> leidos.put(campo.nombre(), campo.leer(v, caja)));
        return leidos;
    }

    Optional<JTextField> caja(String tipo, CampoBeneficio campo) {
        return Optional.ofNullable(camposPorTipo.getOrDefault(tipo, Map.of()).get(campo));
    }

    void reiniciar() {
        camposPorTipo.values().forEach(cajas -> cajas.forEach(CampoBeneficio::reiniciar));
    }
}
