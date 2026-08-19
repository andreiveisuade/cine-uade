package ar.uade.cine.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasSalas;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.dto.salas.PedidoEstadoDTO;
import ar.uade.cine.dto.salas.PedidoSalaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.service.salas.GestorSalas;

/**
 * ABM de salas y estado de las butacas. La distribución llega como lista —[8, 10, 12]
 * es fila A con 8, B con 10 y C con 12— y las butacas que no son estándar vienen por
 * código en tres listas, que es como el gestor espera el mapa de especiales.
 */
@RestController
public class SalaController {

    private final GestorSalas salas;
    private final VistasSalas vistas;

    public SalaController(GestorSalas salas, VistasSalas vistas) {
        this.salas = salas;
        this.vistas = vistas;
    }

    @GetMapping("/api/salas")
    public List<SalaVistaDTO> listar() {
        return salas.listar().stream().map(vistas::sala).toList();
    }

    @GetMapping("/api/salas/{id}")
    public SalaVistaDTO detalle(@PathVariable int id) {
        return vistas.salaConButacas(buscar(id));
    }

    @PostMapping("/api/salas")
    @ResponseStatus(HttpStatus.CREATED)
    public SalaVistaDTO agregar(@RequestBody PedidoSalaDTO pedido) {
        // Sin minutosLimpieza queda el default de la sala: el ABM viejo del front no lo
        // manda, y omitirlo tiene que seguir siendo un alta válida.
        Sala sala = salas.agregar(pedido.nombre(),
                pedido.tipo() == null
                        ? null : Parseo.constante(TipoSala.class, pedido.tipo(), "el tipo de sala"),
                pedido.butacasPorFila(),
                especiales(pedido),
                pedido.minutosLimpieza() == null
                        ? Sala.LIMPIEZA_POR_DEFECTO : pedido.minutosLimpieza());
        return vistas.salaConButacas(sala);
    }

    @DeleteMapping("/api/salas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        buscar(id);
        salas.eliminar(id);
    }

    /**
     * Una butaca rota deja de venderse en todas las funciones, presentes y futuras: por eso
     * el estado es del asiento y no de la reserva (R9).
     */
    @PutMapping("/api/salas/{salaId}/asientos/{codigo}")
    public SalaVistaDTO cambiarEstado(@PathVariable int salaId, @PathVariable String codigo,
                                      @RequestBody PedidoEstadoDTO pedido) {
        buscar(salaId);

        EstadoAsiento estado = Parseo.constante(EstadoAsiento.class, pedido.estado(),
                "el estado de la butaca");
        if (estado == EstadoAsiento.FUERA_DE_SERVICIO) {
            salas.marcarFueraDeServicio(salaId, codigo);
        } else {
            salas.reponer(salaId, codigo);
        }
        return vistas.salaConButacas(buscar(salaId));
    }

    private Sala buscar(int id) {
        return salas.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la sala " + id));
    }

    /** Cada butaca es estándar salvo que su código esté en alguna de las tres listas. */
    private static Map<String, TipoAsiento> especiales(PedidoSalaDTO pedido) {
        Map<String, TipoAsiento> especiales = new HashMap<>();
        marcar(especiales, pedido.codigosVip(), TipoAsiento.VIP);
        marcar(especiales, pedido.codigosPareja(), TipoAsiento.PAREJA);
        marcar(especiales, pedido.codigosAccesibles(), TipoAsiento.ACCESIBLE);
        return especiales;
    }

    private static void marcar(Map<String, TipoAsiento> especiales, List<String> codigos,
                               TipoAsiento tipo) {
        if (codigos == null) {
            return;
        }
        for (String codigo : codigos) {
            if (codigo != null && !codigo.isBlank()) {
                especiales.put(codigo.trim().toUpperCase(), tipo);
            }
        }
    }
}
