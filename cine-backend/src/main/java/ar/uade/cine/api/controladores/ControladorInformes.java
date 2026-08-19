package ar.uade.cine.api.controladores;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.api.http.Fechas;
import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.dto.ventas.BorderoVistaDTO;
import ar.uade.cine.dto.ventas.InformeFuncionVistaDTO;
import ar.uade.cine.dto.ventas.TotalTarifaDTO;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.informes.Bordero;
import ar.uade.cine.servicio.informes.GestorInformes;
import ar.uade.cine.servicio.informes.InformeFuncion;

/**
 * Los informes que se cortan por función: el borderó del INCAA y la recaudación completa.
 *
 * <p>Cuelgan de <code>/api/funciones/{id}</code> porque son informes <em>de una función</em>
 * —igual que el mapa de butacas— pero los atiende este controlador y no
 * {@link ControladorFunciones}: se agrupa por lo que se pide, no por el prefijo de la URL,
 * y estos dos los resuelve un gestor distinto.
 *
 * <p>Arma sus DTO acá adentro en vez de delegar en un {@code Vistas*}, con el mismo criterio
 * que las programaciones: un informe se dibuja solo, ya viene calculado del gestor y no
 * necesita pedirle nada a nadie más para completarse.
 */
@RestController
public class ControladorInformes {

    private final GestorInformes informes;
    private final GestorFunciones funciones;

    public ControladorInformes(GestorInformes informes, GestorFunciones funciones) {
        this.informes = informes;
        this.funciones = funciones;
    }

    @GetMapping("/api/funciones/{id}/bordero")
    public BorderoVistaDTO bordero(@PathVariable int id) {
        exigirFuncion(id);
        return vista(informes.borderoDe(id));
    }

    /**
     * POST y no GET porque escribe: emite el archivo que se sube al INCAA en
     * informes/bordero-funcion-&lt;id&gt;.txt. Pedir dos veces el mismo borderó no es lo
     * mismo que declararlo dos veces.
     */
    @PostMapping("/api/funciones/{id}/bordero")
    @ResponseStatus(HttpStatus.CREATED)
    public BorderoVistaDTO exportarBordero(@PathVariable int id) {
        exigirFuncion(id);
        return vista(informes.exportarBordero(id));
    }

    @GetMapping("/api/funciones/{id}/informe")
    public InformeFuncionVistaDTO informe(@PathVariable int id) {
        exigirFuncion(id);
        InformeFuncion informe = informes.informeDe(id);
        return new InformeFuncionVistaDTO(vista(informe.bordero()), informe.comprasCandy(),
                informe.candy().aPesos(), informe.total().aPesos());
    }

    /**
     * Una función que no existe es un 404 y no un 400: el gestor la rechazaría igual, pero
     * con el código de una regla incumplida. Es el mismo chequeo que hace el controlador de
     * pagos antes de cobrar.
     */
    private void exigirFuncion(int id) {
        funciones.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la función " + id));
    }

    private static BorderoVistaDTO vista(Bordero bordero) {
        // TreeMap por lo mismo que el arqueo: el front lista las tarifas en el orden en que
        // vienen, y alfabético es un orden estable.
        Map<String, TotalTarifaDTO> porTarifa = new TreeMap<>();
        bordero.porTarifa().forEach((tarifa, total) ->
                porTarifa.put(tarifa.name(), new TotalTarifaDTO(total.cantidad(), total.total().aPesos())));

        return new BorderoVistaDTO(bordero.funcionId(), bordero.pelicula(), bordero.sala(),
                Fechas.texto(bordero.funcion()), Fechas.texto(bordero.generadoEn()),
                bordero.espectadores(), bordero.recaudacionBruta().aPesos(),
                bordero.descuentos().aPesos(), bordero.recaudacionNeta().aPesos(), porTarifa);
    }
}
