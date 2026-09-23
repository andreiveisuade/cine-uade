package ar.uade.cine.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.catalogos.ClasificacionVistaDTO;
import ar.uade.cine.dto.catalogos.MedioPagoVistaDTO;
import ar.uade.cine.dto.catalogos.TarifaVistaDTO;
import ar.uade.cine.dto.catalogos.TipoSalaVistaDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catálogos", description = "Las listas de constantes que llenan los combos del panel")
@RestController
public class CatalogoController {

    @Operation(summary = "Los géneros con los que se clasifica una película")
    @GetMapping("/api/generos")
    public List<String> generos() {
        return nombres(Genero.values());
    }

    @Operation(summary = "Las clasificaciones por edad")
    @GetMapping("/api/clasificaciones")
    public List<ClasificacionVistaDTO> clasificaciones() {
        return Arrays.stream(Clasificacion.values())
                .map(c -> new ClasificacionVistaDTO(c.name(), c.getEdadMinima()))
                .toList();
    }

    @Operation(summary = "Los tipos de sala y su recargo")
    @GetMapping("/api/tipos-sala")
    public List<TipoSalaVistaDTO> tiposDeSala() {
        return Arrays.stream(TipoSala.values())
                .map(t -> new TipoSalaVistaDTO(t.name(), t.getMultiplicadorPrecio(), t.soportaTresD()))
                .toList();
    }

    @Operation(summary = "Subtitulada o doblada")
    @GetMapping("/api/idiomas")
    public List<String> idiomas() {
        return nombres(Version.values());
    }

    @Operation(summary = "2D, 3D y sus recargos")
    @GetMapping("/api/proyecciones")
    public List<String> proyecciones() {
        return nombres(Proyeccion.values());
    }

    @Operation(summary = "Los medios de pago que acepta la boletería")
    @GetMapping("/api/medios-pago")
    public List<MedioPagoVistaDTO> mediosDePago() {
        return Arrays.stream(MedioPago.values())
                .map(m -> new MedioPagoVistaDTO(m.name(), m.requiereAutorizacion()))
                .toList();
    }

    @Operation(summary = "Las tarifas de entrada y su descuento")
    @GetMapping("/api/tarifas")
    public List<TarifaVistaDTO> tarifas() {
        return Arrays.stream(TipoTarifa.values())
                .map(t -> new TarifaVistaDTO(t.name(), t.getMultiplicadorPrecio(), t.requiereAcreditacion()))
                .toList();
    }

    private static List<String> nombres(Enum<?>[] constantes) {
        return Arrays.stream(constantes).map(Enum::name).toList();
    }
}
