package ar.uade.cine.api.rutas;

import java.util.Arrays;
import java.util.List;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dto.catalogos.ClasificacionVistaDTO;
import ar.uade.cine.dto.catalogos.MedioPagoVistaDTO;
import ar.uade.cine.dto.catalogos.TarifaVistaDTO;
import ar.uade.cine.dto.catalogos.TipoSalaVistaDTO;
import io.javalin.Javalin;

/**
 * Los enums del dominio, tal como están. El front los pide para armar los combos de
 * sus formularios sin tener que repetir las listas de su lado.
 *
 * <p>Tres de ellos exponen además el dato calculado que lleva la constante: con
 * soportaTresD el front puede avisar de R8 antes de mandar la función, y con
 * requiereAutorizacion pide el código de R11 solo cuando hace falta. La validación de
 * verdad la sigue haciendo el gestor.
 */
class RutasCatalogos {

    static void registrar(Javalin app) {
        app.get("/api/generos", ctx -> ctx.json(nombres(Genero.values())));

        app.get("/api/clasificaciones", ctx -> ctx.json(Arrays.stream(Clasificacion.values())
                .map(c -> new ClasificacionVistaDTO(c.name(), c.getEdadMinima()))
                .toList()));

        app.get("/api/tipos-sala", ctx -> ctx.json(Arrays.stream(TipoSala.values())
                .map(t -> new TipoSalaVistaDTO(t.name(), t.getMultiplicadorPrecio(), t.soportaTresD()))
                .toList()));

        // El enum se llama Version en el dominio: es cómo se escucha esta copia, no el
        // idioma hablado de la película. La ruta conserva el nombre que usa el front.
        app.get("/api/idiomas", ctx -> ctx.json(nombres(Version.values())));

        app.get("/api/proyecciones", ctx -> ctx.json(nombres(Proyeccion.values())));

        app.get("/api/medios-pago", ctx -> ctx.json(Arrays.stream(MedioPago.values())
                .map(m -> new MedioPagoVistaDTO(m.name(), m.requiereAutorizacion()))
                .toList()));

        // El multiplicador va porque el front muestra el precio de cada butaca antes de
        // reservar: sin esto tendría que repetir los factores de su lado, y serían dos
        // fuentes de verdad para lo mismo. requiereAcreditacion es lo que le permite
        // avisar "traé el carnet" al elegir la tarifa, y no recién en la puerta.
        app.get("/api/tarifas", ctx -> ctx.json(Arrays.stream(TipoTarifa.values())
                .map(t -> new TarifaVistaDTO(t.name(), t.getMultiplicadorPrecio(), t.requiereAcreditacion()))
                .toList()));
    }

    private static List<String> nombres(Enum<?>[] constantes) {
        return Arrays.stream(constantes).map(Enum::name).toList();
    }
}
