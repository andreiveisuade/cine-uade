package ar.uade.cine.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.servicio.GestorSalas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * ABM de salas y estado de las butacas. La distribución llega como lista —[8, 10, 12]
 * es fila A con 8, B con 10 y C con 12— y las butacas que no son estándar vienen por
 * código en tres listas, que es como el gestor espera el mapa de especiales.
 */
class RutasSalas {

    record PedidoSala(String nombre, String tipo, List<Integer> butacasPorFila,
                      List<String> codigosVip, List<String> codigosPareja,
                      List<String> codigosAccesibles) {
    }

    record PedidoEstado(String estado) {
    }

    static void registrar(Javalin app, GestorSalas salas, VistasSalas vistas) {

        app.get("/api/salas", ctx ->
                ctx.json(salas.listar().stream().map(vistas::sala).toList()));

        app.get("/api/salas/{id}", ctx ->
                ctx.json(vistas.salaConButacas(buscar(salas, Parseo.id(ctx)))));

        app.post("/api/salas", ctx -> {
            PedidoSala pedido = ctx.bodyAsClass(PedidoSala.class);
            Sala sala = salas.agregar(pedido.nombre(),
                    pedido.tipo() == null ? null : Parseo.constante(TipoSala.class, pedido.tipo(), "el tipo de sala"),
                    pedido.butacasPorFila(),
                    especiales(pedido));
            ctx.status(HttpStatus.CREATED).json(vistas.salaConButacas(sala));
        });

        app.delete("/api/salas/{id}", ctx -> {
            int id = Parseo.id(ctx);
            buscar(salas, id);
            salas.eliminar(id);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // Una butaca rota deja de venderse en todas las funciones, presentes y futuras:
        // por eso el estado es del asiento y no de la reserva (R9).
        app.put("/api/salas/{salaId}/asientos/{codigo}", ctx -> {
            int salaId = Parseo.id(ctx, "salaId");
            String codigo = ctx.pathParam("codigo");
            buscar(salas, salaId);

            EstadoAsiento estado = Parseo.constante(EstadoAsiento.class,
                    ctx.bodyAsClass(PedidoEstado.class).estado(), "el estado de la butaca");
            if (estado == EstadoAsiento.FUERA_DE_SERVICIO) {
                salas.marcarFueraDeServicio(salaId, codigo);
            } else {
                salas.reponer(salaId, codigo);
            }
            ctx.json(vistas.salaConButacas(buscar(salas, salaId)));
        });
    }

    private static Sala buscar(GestorSalas salas, int id) {
        return salas.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la sala " + id));
    }

    /** Cada butaca es estándar salvo que su código esté en alguna de las tres listas. */
    private static Map<String, TipoAsiento> especiales(PedidoSala pedido) {
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
