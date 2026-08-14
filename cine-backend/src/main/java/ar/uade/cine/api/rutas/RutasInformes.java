package ar.uade.cine.api.rutas;

import java.util.Map;
import java.util.TreeMap;

import ar.uade.cine.dto.ventas.BorderoVistaDTO;
import ar.uade.cine.dto.ventas.InformeFuncionVistaDTO;
import ar.uade.cine.dto.ventas.TotalTarifaDTO;
import ar.uade.cine.servicio.informes.Bordero;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.informes.GestorInformes;
import ar.uade.cine.servicio.informes.InformeFuncion;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import ar.uade.cine.api.http.Fechas;
import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Los informes que se cortan por función: el borderó del INCAA y la recaudación completa.
 *
 * <p>Cuelgan de <code>/api/funciones/{id}</code> porque son informes <em>de una función</em>
 * —igual que el mapa de butacas— pero se registran acá y no en {@code RutasFunciones}: la
 * ruta agrupa por lo que se pide, no por el prefijo de la URL, y estos dos los atiende un
 * gestor distinto.
 *
 * <p>Arman sus DTO adentro de la ruta en vez de delegar en un {@code Vistas*}, con el mismo
 * criterio que {@code RutasProgramaciones}: un informe se dibuja solo, ya viene calculado
 * del gestor y no necesita pedirle nada a nadie más para completarse.
 */
class RutasInformes {

    static void registrar(Javalin app, GestorInformes informes, GestorFunciones funciones) {

        app.get("/api/funciones/{id}/bordero", ctx -> {
            int funcionId = existente(funciones, ctx);
            ctx.json(vista(informes.borderoDe(funcionId)));
        });

        // POST y no GET porque escribe: emite el archivo que se sube al INCAA en
        // informes/bordero-funcion-<id>.txt. Pedir dos veces el mismo borderó no es lo
        // mismo que declararlo dos veces.
        app.post("/api/funciones/{id}/bordero", ctx -> {
            int funcionId = existente(funciones, ctx);
            ctx.status(HttpStatus.CREATED).json(vista(informes.exportarBordero(funcionId)));
        });

        app.get("/api/funciones/{id}/informe", ctx -> {
            int funcionId = existente(funciones, ctx);
            InformeFuncion informe = informes.informeDe(funcionId);
            ctx.json(new InformeFuncionVistaDTO(vista(informe.bordero()), informe.comprasCandy(),
                    informe.candy().aPesos(), informe.total().aPesos()));
        });
    }

    /**
     * Una función que no existe es un 404 y no un 400: el gestor la rechazaría igual, pero
     * con el código de una regla incumplida. Es el mismo chequeo que hace RutasPagos antes
     * de cobrar.
     */
    private static int existente(GestorFunciones funciones, io.javalin.http.Context ctx) {
        int funcionId = Parseo.id(ctx);
        funciones.buscar(funcionId)
                .orElseThrow(() -> new NoEncontrado("No existe la función " + funcionId));
        return funcionId;
    }

    private static BorderoVistaDTO vista(Bordero bordero) {
        // TreeMap por lo mismo que el arqueo: el front lista las tarifas en el orden en que
        // vienen, y alfabético es un orden estable.
        Map<String, TotalTarifaDTO> porTarifa = new TreeMap<>();
        bordero.porTarifa().forEach((tarifa, total) ->
                porTarifa.put(tarifa.name(), new TotalTarifaDTO(total.cantidad(), total.total().aPesos())));

        return new BorderoVistaDTO(bordero.funcionId(), bordero.pelicula(), bordero.sala(),
                Fechas.texto(bordero.funcion()), Fechas.texto(bordero.generadoEn()),
                bordero.espectadores(), bordero.recaudacionBruta().aPesos(), bordero.descuentos().aPesos(),
                bordero.recaudacionNeta().aPesos(), porTarifa);
    }
}
