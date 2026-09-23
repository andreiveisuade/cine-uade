package ar.uade.cine.controller;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.dto.ventas.BorderoVistaDTO;
import ar.uade.cine.dto.ventas.InformeFuncionVistaDTO;
import ar.uade.cine.dto.ventas.TotalTarifaDTO;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.Bordero;
import ar.uade.cine.service.informes.GestorInformes;
import ar.uade.cine.service.informes.InformeFuncion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Informes", description = "El borderó del INCAA y la recaudación por función")
@RestController
public class InformeController {

    private final GestorInformes informes;
    private final GestorFunciones funciones;

    public InformeController(GestorInformes informes, GestorFunciones funciones) {
        this.informes = informes;
        this.funciones = funciones;
    }

    @Operation(summary = "El borderó de una función")
    @GetMapping("/api/funciones/{id}/bordero")
    public BorderoVistaDTO bordero(@PathVariable int id) {
        exigirFuncion(id);
        return vista(informes.borderoDe(id));
    }

    @Operation(summary = "Emitir el archivo del borderó que se sube al INCAA")
    @PostMapping("/api/funciones/{id}/bordero")
    @ResponseStatus(HttpStatus.CREATED)
    public BorderoVistaDTO exportarBordero(@PathVariable int id) {
        exigirFuncion(id);
        return vista(informes.exportarBordero(id));
    }

    @Operation(summary = "La recaudación completa de una función: entradas y candy")
    @GetMapping("/api/funciones/{id}/informe")
    public InformeFuncionVistaDTO informe(@PathVariable int id) {
        exigirFuncion(id);
        InformeFuncion informe = informes.informeDe(id);
        return new InformeFuncionVistaDTO(vista(informe.bordero()), informe.comprasCandy(),
                informe.candy().aPesos(), informe.total().aPesos());
    }

    // Se chequea acá para responder 404 y no el 400 del gestor.
    private void exigirFuncion(int id) {
        funciones.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la función " + id));
    }

    private static BorderoVistaDTO vista(Bordero bordero) {
        // TreeMap: el front lista las tarifas en el orden en que llegan.
        Map<String, TotalTarifaDTO> porTarifa = new TreeMap<>();
        bordero.porTarifa().forEach((tarifa, total) ->
                porTarifa.put(tarifa.name(), new TotalTarifaDTO(total.cantidad(), total.total().aPesos())));

        return new BorderoVistaDTO(bordero.funcionId(), bordero.pelicula(), bordero.sala(),
                Fechas.texto(bordero.funcion()), Fechas.texto(bordero.generadoEn()),
                bordero.espectadores(), bordero.recaudacionBruta().aPesos(),
                bordero.descuentos().aPesos(), bordero.recaudacionNeta().aPesos(), porTarifa);
    }
}
