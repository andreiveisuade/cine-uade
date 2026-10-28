package ar.uade.cine.controller.salas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.service.salas.GestorSalas;

// Arma los JSON de salas y butacas; el precio de cada butaca se lo pide a la función (Assembler).
@Component
@RequiredArgsConstructor
public class VistasSalas {

    private final GestorSalas salas;

    public SalaVistaDTO sala(Sala s) {
        return sala(s, salas.asientosDe(s.getId()));
    }

    public List<SalaVistaDTO> salas(List<Sala> lista) {
        Map<Integer, List<Asiento>> asientos = salas.asientosDe(lista.stream().map(Sala::getId).toList());
        return lista.stream().map(s -> sala(s, asientos.getOrDefault(s.getId(), List.of()))).toList();
    }

    public SalaVistaDTO sala(Sala s, List<Asiento> asientos) {
        return armar(s, asientos, null);
    }

    public SalaVistaDTO salaConButacas(Sala s) {
        List<Asiento> asientos = salas.asientosDe(s.getId());
        return armar(s, asientos, asientos.stream().map(this::asiento).toList());
    }

    private SalaVistaDTO armar(Sala s, List<Asiento> asientos, List<AsientoVistaDTO> detalle) {
        List<Integer> distribucion = butacasPorFila(asientos);
        return new SalaVistaDTO(s.getId(), s.getNombre(), s.getTipo().name(), distribucion,
                distribucion.size(), asientos.size(), s.getMinutosLimpieza(), detalle);
    }

    private List<Integer> butacasPorFila(List<Asiento> asientos) {
        return asientos.stream()
                .collect(Collectors.groupingBy(Asiento::getFila, TreeMap::new, Collectors.counting()))
                .values().stream()
                .map(Long::intValue)
                .toList();
    }

    private AsientoVistaDTO asiento(Asiento a) {
        return new AsientoVistaDTO(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(), null, null, null);
    }

    public AsientoVistaDTO asiento(Asiento a, Funcion funcion, Sala sala, Set<Integer> ocupados) {
        return new AsientoVistaDTO(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(),
                ocupados.contains(a.getId()),
                funcion.precioDe(a, sala).aPesos(),
                preciosPorTarifa(funcion.precioDe(a, sala)));
    }

    // Misma cuenta que la entrada al venderse (TipoTarifa.aplicarA), en el orden del enum.
    private Map<String, Double> preciosPorTarifa(Dinero precioDeLaButaca) {
        Map<String, Double> precios = new LinkedHashMap<>();
        for (TipoTarifa t : TipoTarifa.values()) {
            precios.put(t.name(), t.aplicarA(precioDeLaButaca).aPesos());
        }
        return precios;
    }
}
