package ar.uade.cine.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasPromociones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.PedidoActivacionDTO;
import ar.uade.cine.dto.promociones.PedidoPromocionDTO;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;
import ar.uade.cine.service.promociones.CondicionesPromocion;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Promociones", description = "Los descuentos que el cine carga desde el panel")
@RestController
public class PromocionController {

    private final GestorPromociones promociones;
    private final VistasPromociones vistas;

    public PromocionController(GestorPromociones promociones, VistasPromociones vistas) {
        this.promociones = promociones;
        this.vistas = vistas;
    }

    @Operation(summary = "Las promociones cargadas")
    @GetMapping("/api/promociones")
    public List<PromocionVistaDTO> listar() {
        return promociones.listar().stream().map(vistas::promocion).toList();
    }

    @Operation(summary = "El detalle de una promoción")
    @GetMapping("/api/promociones/{id}")
    public PromocionVistaDTO detalle(@PathVariable int id) {
        return vistas.promocion(buscar(id));
    }

    @Operation(summary = "Cargar una promoción")
    @PostMapping("/api/promociones")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PromocionVistaDTO> crear(@Valid @RequestBody PedidoPromocionDTO pedido) {
        LocalDate desde = Parseo.dia(pedido.vigenciaDesde(), "el inicio de la vigencia");
        LocalDate hasta = Parseo.dia(pedido.vigenciaHasta(), "el fin de la vigencia");
        Set<DayOfWeek> dias = new LinkedHashSet<>(Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "el día"));
        Set<MedioPago> medios = new LinkedHashSet<>(Parseo.constantes(MedioPago.class, pedido.mediosPago(), "el medio de pago"));
        LocalTime horaDesde = pedido.horaDesde() == null ? null : Parseo.hora(pedido.horaDesde(), "la hora de inicio");
        LocalTime horaHasta = pedido.horaHasta() == null ? null : Parseo.hora(pedido.horaHasta(), "la hora de fin");
        CondicionesPromocion condiciones = new CondicionesPromocion(desde, hasta, dias,
                horaDesde, horaHasta, medios);

        Promocion promocion = switch (tipoDe(pedido.tipo())) {
            case PORCENTAJE -> promociones.crearPorcentaje(pedido.nombre(),
                    valorObligatorio(pedido.porcentaje(), "porcentaje"), condiciones);
            case MONTO_FIJO -> promociones.crearMontoFijo(pedido.nombre(),
                    Dinero.de(valorObligatorio(pedido.monto(), "monto")), condiciones);
            case NXM -> promociones.crearNxM(pedido.nombre(),
                    (int) valorObligatorio(pedido.lleva() == null ? null : pedido.lleva().doubleValue(), "lleva"),
                    (int) valorObligatorio(pedido.paga() == null ? null : pedido.paga().doubleValue(), "paga"),
                    condiciones);
        };
        return Creado.en("/api/promociones/" + promocion.getId(), vistas.promocion(promocion));
    }

    @Operation(summary = "Dar de baja una promoción sin borrarla, o reactivarla")
    @PatchMapping("/api/promociones/{id}")
    public PromocionVistaDTO cambiarActivacion(@PathVariable int id,
                                               @Valid @RequestBody PedidoActivacionDTO pedido) {
        buscar(id);
        if (pedido.activa()) {
            promociones.activar(id);
        } else {
            promociones.desactivar(id);
        }
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

    private Promocion buscar(int id) {
        return promociones.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la promoción " + id));
    }
}
