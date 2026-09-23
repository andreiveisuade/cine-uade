package ar.uade.cine.controller.vistas;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.salas.GestorSalas;

@Component
public class VistasSalas {

    private final GestorSalas salas;
    private final CalculadoraPrecio calculadora;

    public VistasSalas(GestorSalas salas, CalculadoraPrecio calculadora) {
        this.salas = salas;
        this.calculadora = calculadora;
    }

    public SalaVistaDTO sala(Sala s) {
        return sala(s, salas.asientosDe(s.getId()));
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
                a.getTipo().name(), a.getEstado().name(), null, null);
    }

    AsientoVistaDTO asiento(Asiento a, Funcion funcion, Sala sala, Set<Integer> ocupados) {
        return new AsientoVistaDTO(a.getId(), a.getSalaId(), a.getFila(), a.getNumero(), a.getCodigo(),
                a.getTipo().name(), a.getEstado().name(),
                ocupados.contains(a.getId()),
                calculadora.precioDe(funcion, sala, a, TipoTarifa.GENERAL).aPesos());
    }
}
