package ar.uade.cine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasInformes;
import ar.uade.cine.dto.ventas.BorderoVistaDTO;
import ar.uade.cine.dto.ventas.DeclaracionJuradaVistaDTO;
import ar.uade.cine.dto.ventas.InformeFuncionVistaDTO;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.GestorInformes;
import ar.uade.cine.service.informes.InformeFuncion;
import ar.uade.cine.service.RecursoNoEncontrado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Informes", description = "El borderó del INCAA, la declaración jurada del período y la recaudación por función")
@RestController
public class InformeController {

    private final GestorInformes informes;
    private final GestorFunciones funciones;
    private final VistasInformes vistas;

    public InformeController(GestorInformes informes, GestorFunciones funciones,
                             VistasInformes vistas) {
        this.informes = informes;
        this.funciones = funciones;
        this.vistas = vistas;
    }

    @Operation(summary = "El borderó de una función")
    @GetMapping("/api/funciones/{id}/bordero")
    public BorderoVistaDTO bordero(@PathVariable int id) {
        exigirFuncion(id);
        return vistas.bordero(informes.borderoDe(id));
    }

    @Operation(summary = "La recaudación completa de una función: entradas y candy")
    @GetMapping("/api/funciones/{id}/informe")
    public InformeFuncionVistaDTO informe(@PathVariable int id) {
        exigirFuncion(id);
        InformeFuncion informe = informes.informeDe(id);
        return new InformeFuncionVistaDTO(vistas.bordero(informe.bordero()), informe.comprasCandy(),
                informe.candy().aPesos(), informe.total().aPesos());
    }

    @Operation(summary = "La declaración jurada de un período. Sin fechas, la última semana cinematográfica")
    @GetMapping("/api/declaracion-jurada")
    public DeclaracionJuradaVistaDTO declaracionJurada(@RequestParam(required = false) String desde,
                                                       @RequestParam(required = false) String hasta) {
        return vistas.declaracionJurada(informes.declaracionJurada(
                Parseo.diaOpcional(desde, "la fecha desde"), Parseo.diaOpcional(hasta, "la fecha hasta")));
    }

    // Se chequea acá para responder 404 y no el 400 del gestor.
    private void exigirFuncion(int id) {
        funciones.buscar(id).orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + id));
    }
}
