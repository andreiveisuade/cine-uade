package ar.uade.cine.controller.informes;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.dto.informes.BorderoVistaDTO;
import ar.uade.cine.dto.informes.DeclaracionJuradaVistaDTO;
import ar.uade.cine.dto.informes.InformeFuncionVistaDTO;
import ar.uade.cine.service.informes.GestorInformes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas del borderó, el informe por función y la declaración jurada del INCAA; delega en GestorInformes.
@Tag(name = "Informes", description = "El borderó del INCAA, la declaración jurada del período y la recaudación por función")
@RestController
@RequiredArgsConstructor
public class InformeController {

    private final GestorInformes informes;
    private final VistasInformes vistas;

    @Operation(summary = "El borderó de una función")
    @GetMapping("/api/funciones/{id}/bordero")
    public BorderoVistaDTO bordero(@PathVariable int id) {
        return vistas.bordero(informes.borderoDe(id));
    }

    @Operation(summary = "La recaudación completa de una función: entradas y candy")
    @GetMapping("/api/funciones/{id}/informe")
    public InformeFuncionVistaDTO informe(@PathVariable int id) {
        return vistas.informe(informes.informeDe(id));
    }

    @Operation(summary = "La declaración jurada de un período. Sin fechas, la última semana cinematográfica")
    @GetMapping("/api/declaracion-jurada")
    public DeclaracionJuradaVistaDTO declaracionJurada(@RequestParam(required = false) String desde,
                                                       @RequestParam(required = false) String hasta) {
        return vistas.declaracionJurada(informes.declaracionJurada(
                Parseo.diaOpcional(desde, "la fecha desde"), Parseo.diaOpcional(hasta, "la fecha hasta")));
    }
}
