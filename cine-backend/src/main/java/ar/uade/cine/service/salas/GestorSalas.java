package ar.uade.cine.service.salas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.EstadoAsiento;
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
        if (salaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }

        Sala sala = new Sala(nombre, tipo, minutosLimpieza);
        salaRepository.save(sala);
        asientoRepository.saveAll(generarAsientos(sala.getId(), butacasPorFila, especiales));
        return sala;
    }

    /**
     * Edita nombre, tipo y limpieza. Las butacas no se tocan: ver {@link Sala#editar}.
     *
     * <p>El tipo no cambia si la sala ya tiene funciones, por el mismo motivo que R12 no
     * la deja borrar: una función 3D quedaría en una sala que no la puede proyectar, y el
     * precio de las entradas que faltan vender cambiaría con la función ya publicada.
     */
    public Sala editar(int id, String nombre, TipoSala tipo, Integer minutosLimpieza) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + id));
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de sala");
        }
        int limpieza = minutosLimpieza == null ? sala.getMinutosLimpieza() : minutosLimpieza;
        if (limpieza < 0) {
            throw new IllegalArgumentException("Los minutos de limpieza no pueden ser negativos");
        }
        if (!sala.getNombre().equalsIgnoreCase(nombre.trim())
                && salaRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }
        if (tipo != sala.getTipo() && funcionRepository.existsBySalaId(id)) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: no se le puede cambiar el tipo");
        }
        sala.editar(nombre.trim(), tipo, limpieza);
        return salaRepository.save(sala);
    }

    private List<Asiento> generarAsientos(int salaId, List<Integer> distribucion,
                                          Map<String, TipoAsiento> especiales) {
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= distribucion.size(); fila++) {
            for (int numero = 1; numero <= distribucion.get(fila - 1); numero++) {
                String codigo = Asiento.codigoDe(fila, numero);
                asientos.add(new Asiento(salaId, fila, numero,
                        especiales.getOrDefault(codigo, TipoAsiento.ESTANDAR)));
            }
        }
        return asientos;
    }

    /** Una butaca rota deja de venderse en todas las funciones, presentes y futuras. */
    public void marcarFueraDeServicio(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.FUERA_DE_SERVICIO);
    }

    public void reponer(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.HABILITADO);
    }

    private void cambiarEstado(int salaId, String codigo, EstadoAsiento estado) {
        Asiento asiento = Asiento.conCodigo(asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(salaId), codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en la sala " + salaId));
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
        if (!salaRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe la sala " + id);
        }
        if (funcionRepository.existsBySalaId(id)) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: primero hay que eliminarlas");
        }
        salaRepository.deleteById(id);
    }
}
