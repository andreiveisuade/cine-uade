package ar.uade.cine.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.controller.vistas.VistasPromociones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.promociones.PedidoPromocionDTO;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;
import ar.uade.cine.service.promociones.GestorPromociones;

/**
 * CU-17: el ABM de promociones del administrador. Es lo que justifica que Promocion sea
 * una entidad y no tres constantes en el código: si el cine no las puede cargar desde el
 * sistema, no hacía falta modelarla.
 */
@RestController
public class PromocionController {

    private final GestorPromociones promociones;
    private final VistasPromociones vistas;

    public PromocionController(GestorPromociones promociones, VistasPromociones vistas) {
        this.promociones = promociones;
        this.vistas = vistas;
    }

    @GetMapping("/api/promociones")
    public List<PromocionVistaDTO> listar() {
        return promociones.listar().stream().map(vistas::promocion).toList();
    }

    @GetMapping("/api/promociones/{id}")
    public PromocionVistaDTO detalle(@PathVariable int id) {
        return vistas.promocion(buscar(id));
    }

    @PostMapping("/api/promociones")
    @ResponseStatus(HttpStatus.CREATED)
    public PromocionVistaDTO crear(@RequestBody PedidoPromocionDTO pedido) {
        LocalDate desde = LocalDate.parse(pedido.vigenciaDesde());
        LocalDate hasta = LocalDate.parse(pedido.vigenciaHasta());
        Set<DayOfWeek> dias = leer(pedido.diasSemana(), DayOfWeek::valueOf);
        Set<MedioPago> medios = leer(pedido.mediosPago(), MedioPago::valueOf);
        LocalTime horaDesde = pedido.horaDesde() == null ? null : LocalTime.parse(pedido.horaDesde());
        LocalTime horaHasta = pedido.horaHasta() == null ? null : LocalTime.parse(pedido.horaHasta());

        Promocion promocion = switch (tipoDe(pedido.tipo())) {
            case PORCENTAJE -> promociones.crearPorcentaje(pedido.nombre(),
                    valorObligatorio(pedido.porcentaje(), "porcentaje"),
                    desde, hasta, dias, horaDesde, horaHasta, medios);
            case MONTO_FIJO -> promociones.crearMontoFijo(pedido.nombre(),
                    Dinero.de(valorObligatorio(pedido.monto(), "monto")),
                    desde, hasta, dias, horaDesde, horaHasta, medios);
            case NXM -> promociones.crearNxM(pedido.nombre(),
                    (int) valorObligatorio(pedido.lleva() == null ? null : pedido.lleva().doubleValue(), "lleva"),
                    (int) valorObligatorio(pedido.paga() == null ? null : pedido.paga().doubleValue(), "paga"),
                    desde, hasta, dias, horaDesde, horaHasta, medios);
        };
        return vistas.promocion(promocion);
    }

    /**
     * No hay DELETE: una promoción usada en un cobro tiene que seguir existiendo para poder
     * explicar por qué se cobró ese monto. Se da de baja, no se borra.
     */
    @PostMapping("/api/promociones/{id}/baja")
    public PromocionVistaDTO desactivar(@PathVariable int id) {
        buscar(id);
        promociones.desactivar(id);
        return vistas.promocion(buscar(id));
    }

    @PostMapping("/api/promociones/{id}/alta")
    public PromocionVistaDTO activar(@PathVariable int id) {
        buscar(id);
        promociones.activar(id);
        return vistas.promocion(buscar(id));
    }

    private static TipoPromocion tipoDe(String tipo) {
        try {
            return TipoPromocion.valueOf(tipo == null ? "" : tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo tiene que ser PORCENTAJE, MONTO_FIJO o NXM");
        }
    }

    private static double valorObligatorio(Double valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Falta " + campo + " para ese tipo de promoción");
        }
        return valor;
    }

    /** Una lista ausente o vacía significa sin restricción, no "ninguno". */
    private static <T> Set<T> leer(List<String> valores, Function<String, T> aEnum) {
        Set<T> conjunto = new LinkedHashSet<>();
        if (valores != null) {
            valores.forEach(valor -> conjunto.add(aEnum.apply(valor.trim().toUpperCase())));
        }
        return conjunto;
    }

    private Promocion buscar(int id) {
        return promociones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la promoción " + id));
    }
}
