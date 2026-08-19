package ar.uade.cine.api.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.api.http.Fechas;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dto.cartelera.EstadoImportadorDTO;
import ar.uade.cine.dto.cartelera.ImportacionVistaDTO;
import ar.uade.cine.dto.cartelera.PedidoImportacionDTO;
import ar.uade.cine.infraestructura.importador.CatalogoExterno;
import ar.uade.cine.servicio.cartelera.GestorImportaciones;

/**
 * Pedir cartelera nueva desde el panel.
 *
 * <p>El POST tarda lo que tarda la corrida —diez, quince segundos— y contesta cuando
 * terminó. No devuelve 202 con un id para ir a preguntar después: eso obligaría al navegador
 * a repreguntar cada dos segundos, o sea a hacer treinta pedidos para enterarse de algo que
 * este puede contar de una. nginx tiene un timeout más largo para esta ruta justamente por
 * eso.
 *
 * <p>No hay {@code /{id}}: una importación no tiene pantalla propia. Se la ve en el
 * historial, que es donde tiene sentido —al lado de las anteriores— y de a veinte.
 */
@RestController
public class ControladorImportaciones {

    private final GestorImportaciones importaciones;

    public ControladorImportaciones(GestorImportaciones importaciones) {
        this.importaciones = importaciones;
    }

    @GetMapping("/api/importaciones/estado")
    public EstadoImportadorDTO estado() {
        CatalogoExterno.Estado estado = importaciones.estadoDelImportador();
        return new EstadoImportadorDTO(estado.disponible(), estado.detalle());
    }

    @GetMapping("/api/importaciones")
    public List<ImportacionVistaDTO> listar() {
        return importaciones.listar().stream().map(ControladorImportaciones::vista).toList();
    }

    /**
     * POST y no GET aunque parezca una consulta: gasta llamadas a TMDB y deja películas en
     * el buzón. Es el mismo criterio que /api/acceso.
     *
     * <p>El cuerpo es opcional —{@code required = false}— porque «traeme cartelera» es un
     * pedido completo y el default de páginas lo pone el gestor. Sin eso, un POST vacío
     * moriría en el parseo del JSON y el error saldría con otra forma que la del contrato.
     */
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
