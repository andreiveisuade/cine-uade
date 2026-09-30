package ar.uade.cine.controller.cartelera;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.cartelera.EstadoImportadorVistaDTO;
import ar.uade.cine.dto.cartelera.ImportacionVistaDTO;
import ar.uade.cine.dto.cartelera.PedidoImportacionDTO;
import ar.uade.cine.service.cartelera.GestorImportaciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/importaciones: corre el importador de TMDB y su historial; delega en GestorImportaciones.
// Contesta al terminar la corrida (10-15 s): nginx tiene un timeout más largo para esta ruta.
@Tag(name = "Importación", description = "La cartelera que baja de TMDB")
@RestController
@RequiredArgsConstructor
public class ImportacionController {

    private final GestorImportaciones importaciones;
    private final VistasCartelera vistas;

    @Operation(summary = "Si el importador tiene el token de TMDB para correr; no consulta a TMDB")
    @GetMapping("/api/importaciones/estado")
    public EstadoImportadorVistaDTO estado() {
        return vistas.estado(importaciones.estadoDelImportador());
    }

    @Operation(summary = "El historial de importaciones")
    @GetMapping("/api/importaciones")
    public List<ImportacionVistaDTO> listar() {
        return importaciones.listar().stream().map(vistas::importacion).toList();
    }

    // Alta sin Location: una corrida no tiene GET por id, queda en el historial de GET /api/importaciones.
    @Operation(summary = "Traer cartelera de TMDB. Tarda: contesta cuando terminó")
    @PostMapping("/api/importaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacionVistaDTO importar(
            @RequestBody(required = false) PedidoImportacionDTO pedido) {
        return vistas.importacion(importaciones.ejecutar(pedido == null ? null : pedido.paginas()));
    }
}
