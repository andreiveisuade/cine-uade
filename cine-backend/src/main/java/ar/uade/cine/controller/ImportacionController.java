package ar.uade.cine.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.model.cartelera.Importacion;
import ar.uade.cine.dto.cartelera.EstadoImportadorDTO;
import ar.uade.cine.dto.cartelera.ImportacionVistaDTO;
import ar.uade.cine.dto.cartelera.PedidoImportacionDTO;
import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.service.cartelera.GestorImportaciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Contesta al terminar la corrida (10-15 s): nginx tiene un timeout más largo para esta ruta.
@Tag(name = "Importación", description = "La cartelera que baja de TMDB")
@RestController
public class ImportacionController {

    private final GestorImportaciones importaciones;

    public ImportacionController(GestorImportaciones importaciones) {
        this.importaciones = importaciones;
    }

    @Operation(summary = "Si el importador tiene token y TMDB responde")
    @GetMapping("/api/importaciones/estado")
    public EstadoImportadorDTO estado() {
        CatalogoExterno.Estado estado = importaciones.estadoDelImportador();
        return new EstadoImportadorDTO(estado.disponible(), estado.detalle());
    }

    @Operation(summary = "El historial de importaciones")
    @GetMapping("/api/importaciones")
    public List<ImportacionVistaDTO> listar() {
        return importaciones.listar().stream().map(ImportacionController::vista).toList();
    }

    @Operation(summary = "Traer cartelera de TMDB. Tarda: contesta cuando terminó")
    @PostMapping("/api/importaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacionVistaDTO importar(
            @RequestBody(required = false) PedidoImportacionDTO pedido) {
        return vista(importaciones.ejecutar(pedido == null ? null : pedido.paginas()));
    }

    private static ImportacionVistaDTO vista(Importacion importacion) {
        return new ImportacionVistaDTO(importacion.getId(), importacion.getEstado().name(),
                importacion.getPaginas(), Fechas.texto(importacion.getPedidaEn()),
                Fechas.texto(importacion.getTerminoEn()), importacion.getNuevas(),
                importacion.getSalteadas(), importacion.getFallidas(), importacion.getDetalle());
    }
}
