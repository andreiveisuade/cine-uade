package ar.uade.cine.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.AsientoImpl;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.SalaImpl;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * Reglas de negocio de salas y butacas. Depende de SalaDAO y AsientoDAO por interfaz,
 * y de FuncionDAO solo para R12 (no borrar una sala con funciones programadas).
 */
public class GestorSalas {

    private static final int MAX_FILAS = 26;

    private final SalaDAO salaDAO;
    private final AsientoDAO asientoDAO;
    private final FuncionDAO funcionDAO;

    public GestorSalas(SalaDAO salaDAO, AsientoDAO asientoDAO, FuncionDAO funcionDAO) {
        this.salaDAO = salaDAO;
        this.asientoDAO = asientoDAO;
        this.funcionDAO = funcionDAO;
    }

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        return agregar(nombre, tipo, butacasPorFila, Map.of());
    }

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales) {
        return agregar(nombre, tipo, butacasPorFila, especiales, Sala.LIMPIEZA_POR_DEFECTO);
    }

    /**
     * Crea la sala y genera sus butacas. La distribución es cuántas butacas tiene cada
     * fila de adelante hacia atrás: [8, 10, 12] es fila A con 8, B con 10 y C con 12.
     * No se guarda en la sala, se usa una sola vez acá: a partir de este momento la sala
     * se describe por los asientos que quedaron creados.
     *
     * <p>El mapa marca por código las butacas que no son estándar; el resto lo son.
     *
     * @param minutosLimpieza cuánto hay que esperar entre dos funciones de esta sala. Cero
     *                        es válido y significa que se puede encadenar sin corte
     */
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales, int minutosLimpieza) {
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
        // R2: butacas de cada fila mayores a cero.
        if (butacasPorFila.stream().anyMatch(b -> b == null || b <= 0)) {
            throw new IllegalArgumentException("Cada fila debe tener al menos una butaca");
        }
        // Negativo no es "sin limpieza", es un dato mal cargado: adelantaría el permiso
        // para la función siguiente y la dejaría empezar antes de que termine la anterior.
        if (minutosLimpieza < 0) {
            throw new IllegalArgumentException("Los minutos de limpieza no pueden ser negativos");
        }
        boolean repetida = salaDAO.listar().stream()
                .anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }

        Sala sala = new SalaImpl(nombre, tipo, minutosLimpieza);
        salaDAO.guardar(sala);
        asientoDAO.guardarTodos(generarAsientos(sala.getId(), butacasPorFila, especiales));
        return sala;
    }

    private List<Asiento> generarAsientos(int salaId, List<Integer> distribucion,
                                          Map<String, TipoAsiento> especiales) {
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= distribucion.size(); fila++) {
            for (int numero = 1; numero <= distribucion.get(fila - 1); numero++) {
                String codigo = (char) ('A' + fila - 1) + String.valueOf(numero);
                asientos.add(new AsientoImpl(salaId, fila, numero,
                        especiales.getOrDefault(codigo, TipoAsiento.ESTANDAR)));
            }
        }
        return asientos;
    }

    /** Butacas que tiene la sala. Se cuentan: los asientos son la única fuente de verdad. */
    public int capacidad(int salaId) {
        return asientoDAO.listarPorSala(salaId).size();
    }

    /** Una butaca rota deja de venderse en todas las funciones, presentes y futuras. */
    public void marcarFueraDeServicio(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.FUERA_DE_SERVICIO);
    }

    public void reponer(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.HABILITADO);
    }

    private void cambiarEstado(int salaId, String codigo, EstadoAsiento estado) {
        String buscado = codigo == null ? "" : codigo.trim().toUpperCase();
        Asiento asiento = asientoDAO.listarPorSala(salaId).stream()
                .filter(a -> a.getCodigo().equals(buscado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + buscado + " no existe en la sala " + salaId));
        asiento.setEstado(estado);
        asientoDAO.actualizar(asiento);
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

    /** R12: borrar una sala con funciones programadas dejaría esas funciones sin sala. */
    public void eliminar(int id) {
        if (salaDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la sala " + id);
        }
        if (!funcionDAO.listarPorSala(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: primero hay que eliminarlas");
        }
        salaDAO.eliminar(id);
    }
}
