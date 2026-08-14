package ar.uade.cine.api.rutas;

import ar.uade.cine.api.http.Fechas;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dto.cartelera.EstadoImportadorDTO;
import ar.uade.cine.dto.cartelera.ImportacionVistaDTO;
import ar.uade.cine.dto.cartelera.PedidoImportacionDTO;
import ar.uade.cine.importador.ImportadorCartelera;
import ar.uade.cine.servicio.cartelera.GestorImportaciones;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Pedir cartelera nueva desde el panel.
 *
 * <p>El POST tarda lo que tarda la corrida —diez, quince segundos— y contesta cuando
 * terminó. No devuelve 202 con un id para ir a preguntar después: eso obligaría al
 * navegador a repreguntar cada dos segundos, o sea a hacer treinta pedidos para enterarse
 * de algo que este puede contar de una. nginx tiene un timeout más largo para esta ruta
 * justamente por eso.
 *
 * <p>No hay {@code /{id}}: una importación no tiene pantalla propia. Se la ve en el
 * historial, que es donde tiene sentido —al lado de las anteriores— y de a veinte.
 *
 * <p>Arma el DTO adentro de la ruta y no en una clase {@code Vistas*}, como
 * {@code RutasGrilla}: sale de una sola entidad y no necesita preguntarle nada a ningún
 * otro gestor.
 */
class RutasImportaciones {

    static void registrar(Javalin app, GestorImportaciones importaciones) {

        // Antes que /importaciones/{lo que sea} no hace falta: no hay ninguna otra. Pero el
        // estado sí va antes del listado por costumbre del archivo de al lado.
        app.get("/api/importaciones/estado", ctx -> {
            ImportadorCartelera.Estado estado = importaciones.estadoDelImportador();
            ctx.json(new EstadoImportadorDTO(estado.disponible(), estado.detalle()));
        });

        app.get("/api/importaciones", ctx ->
                ctx.json(importaciones.listar().stream().map(RutasImportaciones::vista).toList()));

        // POST y no GET aunque parezca una consulta: gasta llamadas a TMDB y deja películas
        // en el buzón. Es el mismo criterio que /api/acceso.
        app.post("/api/importaciones", ctx -> {
            // Sin cuerpo también vale: «traeme cartelera» es un pedido completo, y el
            // default de páginas lo pone el gestor. Sin esto, un POST vacío moriría en el
            // parseo del JSON y el error saldría con otra forma que la del contrato.
            PedidoImportacionDTO pedido = ctx.body().isBlank()
                    ? new PedidoImportacionDTO(null)
                    : ctx.bodyAsClass(PedidoImportacionDTO.class);
            ctx.status(HttpStatus.CREATED)
                    .json(vista(importaciones.ejecutar(pedido.paginas())));
        });
    }

    private static ImportacionVistaDTO vista(Importacion importacion) {
        return new ImportacionVistaDTO(importacion.getId(), importacion.getEstado().name(),
                importacion.getPaginas(), Fechas.texto(importacion.getPedidaEn()),
                Fechas.texto(importacion.getTerminoEn()), importacion.getNuevas(),
                importacion.getSalteadas(), importacion.getFallidas(), importacion.getDetalle());
    }
}
