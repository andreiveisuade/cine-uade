package ar.uade.cine.api;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Arma las salas y sus butacas en la forma que espera el front.
 *
 * <p>Necesita GestorSalas porque una sala no sabe cuáles son sus butacas —las guarda el
 * AsientoDAO, no la sala— y la respuesta las incluye. Es justamente lo que el DTO no
 * puede hacer por su cuenta.
 */
public class VistasSalas {

    private final GestorSalas salas;
    private final CalculadoraPrecio calculadora;

    public VistasSalas(GestorSalas salas, CalculadoraPrecio calculadora) {
        this.salas = salas;
        this.calculadora = calculadora;
    }

    /** La sala sola, para embeberla en una función. */
    public SalaVistaDTO sala(Sala s) {
        return sala(s, salas.asientosDe(s.getId()));
    }

    /** Con los asientos ya leídos, para no volver a pedirlos cuando quien llama los tiene. */
    public SalaVistaDTO sala(Sala s, List<Asiento> asientos) {
        return armar(s, asientos, null);
    }

    /** La sala con el detalle de cada butaca, que es lo que necesita el ABM de salas. */
    public SalaVistaDTO salaConButacas(Sala s) {
        List<Asiento> asientos = salas.asientosDe(s.getId());
        return armar(s, asientos, asientos.stream().map(this::asiento).toList());
    }

    private SalaVistaDTO armar(Sala s, List<Asiento> asientos, List<AsientoVistaDTO> detalle) {
        List<Integer> distribucion = butacasPorFila(asientos);
        return new SalaVistaDTO(s.getId(), s.getNombre(), s.getTipo().name(), distribucion,
                distribucion.size(), asientos.size(), s.getMinutosLimpieza(), detalle);
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

    private AsientoVistaDTO asiento(Asiento a) {
        return new AsientoVistaDTO(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(), null, null);
    }

    /**
     * La butaca dentro del mapa de una función: ahí sí se sabe si está tomada y cuánto
     * sale.
     *
     * <p>Muestra el precio de tarifa general: la tarifa de cada persona se elige recién al
     * reservar, y de ahí para abajo el precio solo puede bajar.
     */
    AsientoVistaDTO asiento(Asiento a, Funcion funcion, Sala sala, Set<Integer> ocupados) {
        return new AsientoVistaDTO(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(),
                ocupados.contains(a.getId()),
                calculadora.precioDe(funcion, sala, a, TipoTarifa.GENERAL));
    }
}
