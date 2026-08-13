package ar.uade.cine.api;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Las salas y sus butacas, en la forma que espera el front.
 *
 * <p>Necesita GestorSalas porque una sala no sabe cuáles son sus butacas —las guarda el
 * AsientoDAO, no la sala— y la respuesta las incluye.
 */
public class VistasSalas {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SalaVista(int id, String nombre, String tipo, List<Integer> butacasPorFila,
                            int filas, int capacidadSala, List<AsientoVista> asientos) {
    }

    /**
     * {@code estado} es de la butaca y vale para todas las funciones; {@code ocupado}
     * es de esta función. Van separados porque el front los pinta distinto. Sin función
     * de por medio, ocupado y precio no aplican y no se mandan.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AsientoVista(int id, int salaId, int fila, int numero, String codigo,
                               String tipo, String estado, Boolean ocupado, Double precio) {
    }

    private final GestorSalas salas;
    private final CalculadoraPrecio calculadora;

    public VistasSalas(GestorSalas salas, CalculadoraPrecio calculadora) {
        this.salas = salas;
        this.calculadora = calculadora;
    }

    /** La sala sola, para embeberla en una función. */
    public SalaVista sala(Sala s) {
        return sala(s, salas.asientosDe(s.getId()));
    }

    /** Con los asientos ya leídos, para no volver a pedirlos cuando quien llama los tiene. */
    public SalaVista sala(Sala s, List<Asiento> asientos) {
        return armar(s, asientos, null);
    }

    /** La sala con el detalle de cada butaca, que es lo que necesita el ABM de salas. */
    public SalaVista salaConButacas(Sala s) {
        List<Asiento> asientos = salas.asientosDe(s.getId());
        return armar(s, asientos, asientos.stream().map(this::asiento).toList());
    }

    private SalaVista armar(Sala s, List<Asiento> asientos, List<AsientoVista> detalle) {
        List<Integer> distribucion = butacasPorFila(asientos);
        return new SalaVista(s.getId(), s.getNombre(), s.getTipo().name(), distribucion,
                distribucion.size(), asientos.size(), detalle);
    }

    /**
     * La distribución no se guarda en ningún lado: se reconstruye contando las butacas
     * de cada fila, que son la única fuente de verdad de cómo es la sala.
     */
    private List<Integer> butacasPorFila(List<Asiento> asientos) {
        return asientos.stream()
                .collect(Collectors.groupingBy(Asiento::getFila, TreeMap::new, Collectors.counting()))
                .values().stream()
                .map(Long::intValue)
                .toList();
    }

    private AsientoVista asiento(Asiento a) {
        return new AsientoVista(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(), null, null);
    }

    /**
     * La butaca dentro del mapa de una función: ahí sí se sabe si está tomada y cuánto
     * sale.
     *
     * <p>Muestra el precio de tarifa general: la tarifa de cada persona se elige recién al
     * reservar, y de ahí para abajo el precio solo puede bajar.
     */
    AsientoVista asiento(Asiento a, Funcion funcion, Sala sala, Set<Integer> ocupados) {
        return new AsientoVista(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(),
                ocupados.contains(a.getId()),
                calculadora.precioDe(funcion, sala, a, TipoTarifa.GENERAL));
    }
}
