package ar.uade.cine.api.rutas;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.dominio.promociones.TipoPromocion;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dto.promociones.PedidoPromocionDTO;
import ar.uade.cine.servicio.promociones.GestorPromociones;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.api.vistas.VistasPromociones;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * CU-17: el ABM de promociones del administrador. Es lo que justifica que Promocion sea
 * una entidad y no tres constantes en el código: si el cine no las puede cargar desde el
 * sistema, no hacía falta modelarla.
 */
class RutasPromociones {

    static void registrar(Javalin app, GestorPromociones promociones, VistasPromociones vistas) {

        app.get("/api/promociones", ctx ->
                ctx.json(promociones.listar().stream().map(vistas::promocion).toList()));

        app.get("/api/promociones/{id}", ctx ->
                ctx.json(vistas.promocion(buscar(promociones, Parseo.id(ctx)))));

        app.post("/api/promociones", ctx -> {
            PedidoPromocionDTO pedido = ctx.bodyAsClass(PedidoPromocionDTO.class);
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
            ctx.status(HttpStatus.CREATED).json(vistas.promocion(promocion));
        });

        // No hay DELETE: una promoción usada en un cobro tiene que seguir existiendo
        // para poder explicar por qué se cobró ese monto. Se da de baja, no se borra.
        app.post("/api/promociones/{id}/baja", ctx -> {
            int id = Parseo.id(ctx);
            buscar(promociones, id);
            promociones.desactivar(id);
            ctx.json(vistas.promocion(buscar(promociones, id)));
        });

        app.post("/api/promociones/{id}/alta", ctx -> {
            int id = Parseo.id(ctx);
            buscar(promociones, id);
            promociones.activar(id);
            ctx.json(vistas.promocion(buscar(promociones, id)));
        });
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
    private static <T> Set<T> leer(List<String> valores, java.util.function.Function<String, T> aEnum) {
        Set<T> conjunto = new LinkedHashSet<>();
        if (valores != null) {
            valores.forEach(valor -> conjunto.add(aEnum.apply(valor.trim().toUpperCase())));
        }
        return conjunto;
    }

    private static Promocion buscar(GestorPromociones promociones, int id) {
        return promociones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la promoción " + id));
    }
}
