package ar.uade.cine.controller.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.RequiredArgsConstructor;
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
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.promociones.ParametrosPromocion;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.comun.PedidoActivacionDTO;
import ar.uade.cine.dto.promociones.PedidoPromocionDTO;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;
import ar.uade.cine.service.promociones.GestorPromociones;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/promociones: traduce el pedido a tipo, parámetros y condiciones; las reglas, en el modelo.
@Tag(name = "Promociones", description = "Los descuentos que el cine carga desde el panel")
@RestController
@RequiredArgsConstructor
public class PromocionController {

    private final GestorPromociones promociones;
    private final VistasPromociones vistas;

    @Operation(summary = "Las promociones cargadas")
    @GetMapping("/api/promociones")
    public List<PromocionVistaDTO> listar() {
        return promociones.listar().stream().map(vistas::promocion).toList();
    }

    @Operation(summary = "El detalle de una promoción")
    @GetMapping("/api/promociones/{id}")
    public PromocionVistaDTO detalle(@PathVariable int id) {
        return vistas.promocion(promociones.obtener(id));
    }

    @Operation(summary = "Cargar una promoción")
    @PostMapping("/api/promociones")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PromocionVistaDTO> crear(@Valid @RequestBody PedidoPromocionDTO pedido) {
        LocalDate desde = Parseo.dia(pedido.vigenciaDesde(), "el inicio de la vigencia");
        LocalDate hasta = Parseo.dia(pedido.vigenciaHasta(), "el fin de la vigencia");
        Set<DayOfWeek> dias = new LinkedHashSet<>(Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "el día de la semana"));
        Set<MedioPago> medios = new LinkedHashSet<>(Parseo.constantes(MedioPago.class, pedido.mediosPago(), "el medio de pago"));
        // Una hora vacía es una hora que no vino, como en el resto de la API: la franja queda abierta.
        LocalTime horaDesde = Parseo.horaOpcional(pedido.horaDesde(), "la hora de inicio");
        LocalTime horaHasta = Parseo.horaOpcional(pedido.horaHasta(), "la hora de fin");
        CondicionesPromocion condiciones = new CondicionesPromocion(desde, hasta, dias,
                horaDesde, horaHasta, medios);
        ParametrosPromocion parametros = new ParametrosPromocion(pedido.porcentaje(), pedido.monto(),
                pedido.lleva(), pedido.paga());

        // Sin switch: el tipo crea su subclase (Factory Method en TipoPromocion).
        Promocion promocion = promociones.crear(tipoDe(pedido.tipo()), pedido.nombre(), parametros, condiciones);
        return Creado.en("/api/promociones/" + promocion.getId(), vistas.promocion(promocion));
    }

    @Operation(summary = "Dar de baja una promoción sin borrarla, o reactivarla")
    @PatchMapping("/api/promociones/{id}")
    public PromocionVistaDTO cambiarActivacion(@PathVariable int id,
                                               @Valid @RequestBody PedidoActivacionDTO pedido) {
        Promocion promocion = pedido.activa() ? promociones.activar(id) : promociones.desactivar(id);
        return vistas.promocion(promocion);
    }

    // Que venga lo exige el pedido; acá, que sea uno de los tres. El mensaje nombra las etiquetas y no las
    // constantes, a diferencia de Parseo.constante.
    private static TipoPromocion tipoDe(String tipo) {
        try {
            return TipoPromocion.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatoInvalido("El tipo de promoción tiene que ser porcentaje, monto fijo o NxM");
        }
    }
}
