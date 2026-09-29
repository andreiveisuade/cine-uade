package ar.uade.cine.controller.informes;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.dto.informes.ArqueoCandyVistaDTO;
import ar.uade.cine.dto.informes.ArqueoVistaDTO;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.informes.GestorCaja;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas /api/arqueo y /api/candy/arqueo: el cierre de caja de un día vía GestorCaja; solo lee, no cobra.
// El corte por día (GestorCaja), como la pantalla Caja del panel. Cada arqueo se ve en Swagger
// junto a lo que arquea: por eso el tag va en el método y no en la clase.
@RestController
@RequiredArgsConstructor
public class CajaController {

    private final GestorCaja caja;
    private final GestorCandy candy;
    private final VistasInformes vistas;

    @Tag(name = "Cobros")
    @Operation(summary = "El arqueo de boletería de un día")
    @GetMapping("/api/arqueo")
    public ArqueoVistaDTO arqueo(@RequestParam(required = false) String fecha) {
        return vistas.arqueo(caja.arqueoDe(Parseo.dia(fecha, "la fecha")));
    }

    @Tag(name = "Candy")
    @Operation(summary = "El arqueo del candy de un día")
    @GetMapping("/api/candy/arqueo")
    public ArqueoCandyVistaDTO arqueoCandy(@RequestParam(required = false) String fecha) {
        LocalDate dia = Parseo.dia(fecha, "la fecha");
        return vistas.arqueoCandy(dia, caja.totalCandyDe(dia), candy.listarComprasDelDia(dia));
    }
}
