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

/**
 * Los enums del dominio, tal como están. El front los pide para armar los combos de
 * sus formularios sin tener que repetir las listas de su lado.
 *
 * <p>Tres de ellos exponen además el dato calculado que lleva la constante: con
 * soportaTresD el front puede avisar de R8 antes de mandar la función, y con
 * requiereAutorizacion pide el código de R11 solo cuando hace falta. La validación de
 * verdad la sigue haciendo el gestor.
 *
 * <p>Es el único controlador sin colaboradores: no le pide nada a ningún gestor porque no
 * hay nada que preguntar —las constantes son las que son.
 */
@RestController
public class CatalogoController {

    @GetMapping("/api/generos")
    public List<String> generos() {
        return nombres(Genero.values());
    }

    @GetMapping("/api/clasificaciones")
    public List<ClasificacionVistaDTO> clasificaciones() {
        return Arrays.stream(Clasificacion.values())
                .map(c -> new ClasificacionVistaDTO(c.name(), c.getEdadMinima()))
                .toList();
    }

    @GetMapping("/api/tipos-sala")
    public List<TipoSalaVistaDTO> tiposDeSala() {
        return Arrays.stream(TipoSala.values())
                .map(t -> new TipoSalaVistaDTO(t.name(), t.getMultiplicadorPrecio(), t.soportaTresD()))
                .toList();
    }

    /**
     * El enum se llama Version en el dominio: es cómo se escucha esta copia, no el idioma
     * hablado de la película. La ruta conserva el nombre que usa el front.
     */
    @GetMapping("/api/idiomas")
    public List<String> idiomas() {
        return nombres(Version.values());
    }

    @GetMapping("/api/proyecciones")
    public List<String> proyecciones() {
        return nombres(Proyeccion.values());
    }

    @GetMapping("/api/medios-pago")
    public List<MedioPagoVistaDTO> mediosDePago() {
        return Arrays.stream(MedioPago.values())
                .map(m -> new MedioPagoVistaDTO(m.name(), m.requiereAutorizacion()))
                .toList();
    }

    /**
     * El multiplicador va porque el front muestra el precio de cada butaca antes de
     * reservar: sin esto tendría que repetir los factores de su lado, y serían dos fuentes
     * de verdad para lo mismo. requiereAcreditacion es lo que le permite avisar "traé el
     * carnet" al elegir la tarifa, y no recién en la puerta.
     */
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
