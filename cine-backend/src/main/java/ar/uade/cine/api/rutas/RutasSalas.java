package ar.uade.cine.api.rutas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dto.salas.PedidoEstadoDTO;
import ar.uade.cine.dto.salas.PedidoSalaDTO;
import ar.uade.cine.servicio.salas.GestorSalas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.api.vistas.VistasSalas;

/**
 * ABM de salas y estado de las butacas. La distribución llega como lista —[8, 10, 12]
 * es fila A con 8, B con 10 y C con 12— y las butacas que no son estándar vienen por
 * código en tres listas, que es como el gestor espera el mapa de especiales.
 */
class RutasSalas {

    static void registrar(Javalin app, GestorSalas salas, VistasSalas vistas) {

        app.get("/api/salas", ctx ->
                ctx.json(salas.listar().stream().map(vistas::sala).toList()));

        app.get("/api/salas/{id}", ctx ->
                ctx.json(vistas.salaConButacas(buscar(salas, Parseo.id(ctx)))));

        app.post("/api/salas", ctx -> {
            PedidoSalaDTO pedido = ctx.bodyAsClass(PedidoSalaDTO.class);
            // Sin minutosLimpieza queda el default de la sala: el ABM viejo del front no
            // lo manda, y omitirlo tiene que seguir siendo un alta válida.
            Sala sala = salas.agregar(pedido.nombre(),
                    pedido.tipo() == null ? null : Parseo.constante(TipoSala.class, pedido.tipo(), "el tipo de sala"),
                    pedido.butacasPorFila(),
                    especiales(pedido),
                    pedido.minutosLimpieza() == null
                            ? Sala.LIMPIEZA_POR_DEFECTO : pedido.minutosLimpieza());
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
                    ctx.bodyAsClass(PedidoEstadoDTO.class).estado(), "el estado de la butaca");
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
