package ar.uade.cine.controller.cartelera;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// El buzón de lo que trajo el importador: su propio gestor y su propio caso de uso, aunque las
// rutas cuelguen de /api/peliculas. Sin chequeo previo de 404: GestorRevisionCartelera ya lo da.
@Tag(name = "Películas")
@RestController
@RequiredArgsConstructor
public class RevisionController {

    private final GestorRevisionCartelera revision;
    private final VistasCartelera vistas;

    @Operation(summary = "El buzón: lo que trajo el importador y todavía nadie revisó")
    @GetMapping("/api/peliculas/pendientes")
    public List<PeliculaVistaDTO> pendientes() {
        return revision.listarPendientes().stream().map(vistas::pelicula).toList();
    }

    @Operation(summary = "Aceptar una película del buzón y publicarla")
    @PostMapping("/api/peliculas/{id}/confirmacion")
    public PeliculaVistaDTO confirmar(@PathVariable int id) {
        return vistas.pelicula(revision.confirmar(id));
    }

    @Operation(summary = "Descartar una película del buzón")
    @PostMapping("/api/peliculas/{id}/descarte")
    public PeliculaVistaDTO descartar(@PathVariable int id) {
        return vistas.pelicula(revision.descartar(id));
    }
}
