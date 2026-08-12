package ar.uade.cine.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Asiento;
import ar.uade.cine.interfaces.AsientoDAO;
import ar.uade.cine.interfaces.Sala;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.AsientoImpl;
import ar.uade.cine.modelo.SalaImpl;
import ar.uade.cine.modelo.TipoAsiento;
import ar.uade.cine.modelo.TipoSala;

public class GestorSalas {

    private static final int MAX_FILAS = 26;

    private final SalaDAO salaDAO;
    private final AsientoDAO asientoDAO;

    public GestorSalas(SalaDAO salaDAO, AsientoDAO asientoDAO) {
        this.salaDAO = salaDAO;
        this.asientoDAO = asientoDAO;
    }

    /**
     * Crea la sala y genera sus butacas. La distribución es la cantidad de butacas de
     * cada fila, de adelante hacia atrás. Todas las butacas quedan estándar.
     */
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        return agregar(nombre, tipo, butacasPorFila, List.of(), List.of());
    }

    /**
     * Igual que el anterior, pero marcando butacas especiales por código: las accesibles
     * suelen ir al borde de una fila y las VIP o de pareja en las filas del fondo.
     */
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        List<String> codigosVip, List<String> codigosAccesibles) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de sala");
        }
        if (butacasPorFila == null || butacasPorFila.isEmpty()) {
            throw new IllegalArgumentException("La sala necesita al menos una fila");
        }
        if (butacasPorFila.size() > MAX_FILAS) {
            throw new IllegalArgumentException("Máximo " + MAX_FILAS + " filas: se identifican con una letra");
        }
        if (butacasPorFila.stream().anyMatch(b -> b == null || b <= 0)) {
            throw new IllegalArgumentException("Cada fila debe tener al menos una butaca");
        }
        boolean repetida = salaDAO.listar().stream()
                .anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }

        Sala sala = new SalaImpl(nombre, tipo, butacasPorFila);
        salaDAO.guardar(sala);
        asientoDAO.guardarTodos(generarAsientos(sala, codigosVip, codigosAccesibles));
        return sala;
    }

    private List<Asiento> generarAsientos(Sala sala, List<String> vip, List<String> accesibles) {
        List<Integer> distribucion = sala.getButacasPorFila();
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= distribucion.size(); fila++) {
            for (int numero = 1; numero <= distribucion.get(fila - 1); numero++) {
                String codigo = (char) ('A' + fila - 1) + String.valueOf(numero);
                TipoAsiento tipo = TipoAsiento.ESTANDAR;
                if (vip.contains(codigo)) {
                    tipo = sala.getTipo() == TipoSala.VIP ? TipoAsiento.PAREJA : TipoAsiento.VIP;
                } else if (accesibles.contains(codigo)) {
                    tipo = TipoAsiento.ACCESIBLE;
                }
                asientos.add(new AsientoImpl(sala.getId(), fila, numero, tipo));
            }
        }
        return asientos;
    }

    public List<Sala> listar() {
        return salaDAO.listar();
    }

    public List<Asiento> asientosDe(int salaId) {
        return asientoDAO.listarPorSala(salaId);
    }

    public Optional<Sala> buscar(int id) {
        return salaDAO.buscarPorId(id);
    }

    public void eliminar(int id) {
        if (salaDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la sala " + id);
        }
        salaDAO.eliminar(id);
    }
}
