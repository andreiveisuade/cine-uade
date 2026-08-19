package ar.uade.cine.service.salas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.SalaRepository;

/**
 * Reglas de negocio de salas y butacas. Depende de SalaRepository y AsientoRepository por interfaz,
 * y de FuncionRepository solo para R12 (no borrar una sala con funciones programadas).
 */
@Service
@Transactional
public class GestorSalas {

    private static final int MAX_FILAS = 26;

    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final FuncionRepository funcionRepository;

    public GestorSalas(SalaRepository salaRepository, AsientoRepository asientoRepository, FuncionRepository funcionRepository) {
        this.salaRepository = salaRepository;
        this.asientoRepository = asientoRepository;
        this.funcionRepository = funcionRepository;
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
        boolean repetida = salaRepository.findAll().stream()
                .anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }

        Sala sala = new Sala(nombre, tipo, minutosLimpieza);
        salaRepository.save(sala);
        asientoRepository.saveAll(generarAsientos(sala.getId(), butacasPorFila, especiales));
        return sala;
    }

    private List<Asiento> generarAsientos(int salaId, List<Integer> distribucion,
                                          Map<String, TipoAsiento> especiales) {
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= distribucion.size(); fila++) {
            for (int numero = 1; numero <= distribucion.get(fila - 1); numero++) {
                String codigo = (char) ('A' + fila - 1) + String.valueOf(numero);
                asientos.add(new Asiento(salaId, fila, numero,
                        especiales.getOrDefault(codigo, TipoAsiento.ESTANDAR)));
            }
        }
        return asientos;
    }

    /** Butacas que tiene la sala. Se cuentan: los asientos son la única fuente de verdad. */
    public int capacidad(int salaId) {
        return asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(salaId).size();
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
        Asiento asiento = asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(salaId).stream()
                .filter(a -> a.getCodigo().equals(buscado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + buscado + " no existe en la sala " + salaId));
        asiento.setEstado(estado);
        asientoRepository.save(asiento);
    }

    public List<Sala> listar() {
        return salaRepository.findAll();
    }

    public List<Asiento> asientosDe(int salaId) {
        return asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(salaId);
    }

    public Optional<Sala> buscar(int id) {
        return salaRepository.findById(id);
    }

    /** R12: borrar una sala con funciones programadas dejaría esas funciones sin sala. */
    public void eliminar(int id) {
        if (salaRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la sala " + id);
        }
        if (!funcionRepository.findBySalaId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: primero hay que eliminarlas");
        }
        salaRepository.deleteById(id);
    }
}
