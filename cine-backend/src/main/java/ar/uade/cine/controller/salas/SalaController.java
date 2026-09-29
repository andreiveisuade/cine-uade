package ar.uade.cine.controller.salas;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.dto.salas.PedidoEdicionSalaDTO;
import ar.uade.cine.dto.salas.PedidoEstadoDTO;
import ar.uade.cine.dto.salas.PedidoSalaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.service.salas.GestorSalas;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/salas y butacas fuera de servicio (R9): traduce HTTP a GestorSalas; responde con VistasSalas.
@Tag(name = "Salas", description = "Las salas del cine y el estado de cada butaca")
@RestController
@RequiredArgsConstructor
public class SalaController {

    private final GestorSalas salas;
    private final VistasSalas vistas;

    @Operation(summary = "Las salas del cine")
    @GetMapping("/api/salas")
    public List<SalaVistaDTO> listar() {
        return salas.listar().stream().map(vistas::sala).toList();
    }

    @Operation(summary = "Una sala con todas sus butacas")
    @GetMapping("/api/salas/{id}")
    public SalaVistaDTO detalle(@PathVariable int id) {
        return vistas.salaConButacas(salas.obtener(id));
    }

    @Operation(summary = "Dar de alta una sala y generarle las butacas")
    @PostMapping("/api/salas")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SalaVistaDTO> agregar(@Valid @RequestBody PedidoSalaDTO pedido) {
        Sala sala = salas.agregar(pedido.nombre(),
                Parseo.constante(TipoSala.class, pedido.tipo(), "el tipo de sala"),
                pedido.butacasPorFila(),
                especiales(pedido),
                pedido.minutosLimpieza());
        return Creado.en("/api/salas/" + sala.getId(), vistas.salaConButacas(sala));
    }

    @Operation(summary = "Editar nombre, tipo y limpieza de una sala. Las butacas no cambian")
    @PutMapping("/api/salas/{id}")
    public SalaVistaDTO editar(@PathVariable int id, @Valid @RequestBody PedidoEdicionSalaDTO pedido) {
        Sala sala = salas.editar(id, pedido.nombre(),
                Parseo.constante(TipoSala.class, pedido.tipo(), "el tipo de sala"),
                pedido.minutosLimpieza());
        return vistas.salaConButacas(sala);
    }

    @Operation(summary = "Borrar una sala")
    @DeleteMapping("/api/salas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        salas.eliminar(id);
    }

    @Operation(summary = "Marcar una butaca fuera de servicio, o reponerla")
    @PatchMapping("/api/salas/{salaId}/asientos/{codigo}")
    public SalaVistaDTO cambiarEstado(@PathVariable int salaId, @PathVariable String codigo,
                                      @Valid @RequestBody PedidoEstadoDTO pedido) {
        EstadoAsiento estado = Parseo.constante(EstadoAsiento.class, pedido.estado(),
                "el estado de la butaca");
        return vistas.salaConButacas(estado == EstadoAsiento.FUERA_DE_SERVICIO
                ? salas.marcarFueraDeServicio(salaId, codigo)
                : salas.reponer(salaId, codigo));
    }

    // Las tres listas tal como vinieron, cada una con su tipo: normalizar los códigos y rechazar el que
    // está en dos es cosa de Sala.generarAsientos. EnumMap porque acepta una lista que no vino (null) y
    // recorre siempre en el orden de TipoAsiento.
    private static Map<TipoAsiento, List<String>> especiales(PedidoSalaDTO pedido) {
        Map<TipoAsiento, List<String>> especiales = new EnumMap<>(TipoAsiento.class);
        especiales.put(TipoAsiento.VIP, pedido.codigosVip());
        especiales.put(TipoAsiento.PAREJA, pedido.codigosPareja());
        especiales.put(TipoAsiento.ACCESIBLE, pedido.codigosAccesibles());
        return especiales;
    }
}
