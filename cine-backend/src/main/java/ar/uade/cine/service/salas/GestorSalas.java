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
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

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

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales, int minutosLimpieza) {
        // Antes que las filas y el nombre repetido: los datos de la sala se rechazan primero.
        Sala sala = new Sala(nombre, tipo, minutosLimpieza);
        if (butacasPorFila == null || butacasPorFila.isEmpty()) {
            throw new IllegalArgumentException("La sala necesita al menos una fila");
        }
        if (butacasPorFila.size() > MAX_FILAS) {
            throw new IllegalArgumentException("Máximo " + MAX_FILAS + " filas: se identifican con una letra");
        }
        if (butacasPorFila.stream().anyMatch(b -> b == null || b <= 0)) {
            throw new IllegalArgumentException("Cada fila debe tener al menos una butaca");
        }
        if (salaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }

        salaRepository.save(sala);
        asientoRepository.saveAll(generarAsientos(sala, butacasPorFila, especiales));
        return sala;
    }

    // El tipo no cambia con funciones: una función 3D quedaría en una sala que no la proyecta.
    public Sala editar(int id, String nombre, TipoSala tipo, Integer minutosLimpieza) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la sala " + id));
        int limpieza = minutosLimpieza == null ? sala.getMinutosLimpieza() : minutosLimpieza;
        String nuevoNombre = nombre == null ? null : nombre.trim();
        // Los chequeos contra la base van antes de editar: con la sala ya modificada, la consulta
        // del nombre repetido haría flush y se encontraría a sí misma.
        if (nuevoNombre != null && !nuevoNombre.isEmpty() && !sala.getNombre().equalsIgnoreCase(nuevoNombre)
                && salaRepository.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }
        if (tipo != null && tipo != sala.getTipo() && funcionRepository.existsBySala_Id(id)) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: no se le puede cambiar el tipo");
        }
        sala.editar(nuevoNombre, tipo, limpieza);
        return salaRepository.save(sala);
    }

    private List<Asiento> generarAsientos(Sala sala, List<Integer> distribucion,
                                          Map<String, TipoAsiento> especiales) {
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= distribucion.size(); fila++) {
            for (int numero = 1; numero <= distribucion.get(fila - 1); numero++) {
                String codigo = Asiento.codigoDe(fila, numero);
                asientos.add(new Asiento(sala, fila, numero,
                        especiales.getOrDefault(codigo, TipoAsiento.ESTANDAR)));
            }
        }
        return asientos;
    }

    public void marcarFueraDeServicio(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.FUERA_DE_SERVICIO);
    }

    public void reponer(int salaId, String codigo) {
        cambiarEstado(salaId, codigo, EstadoAsiento.HABILITADO);
    }

    private void cambiarEstado(int salaId, String codigo, EstadoAsiento estado) {
        Asiento asiento = Asiento.conCodigo(asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(salaId), codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en la sala " + salaId));
        asiento.setEstado(estado);
        asientoRepository.save(asiento);
    }

    @Transactional(readOnly = true)
    public List<Sala> listar() {
        return salaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Asiento> asientosDe(int salaId) {
        return asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(salaId);
    }

    @Transactional(readOnly = true)
    public Optional<Sala> buscar(int id) {
        return salaRepository.findById(id);
    }

    public void eliminar(int id) {
        if (!salaRepository.existsById(id)) {
            throw new RecursoNoEncontrado("No existe la sala " + id);
        }
        if (funcionRepository.existsBySala_Id(id)) {
            throw new IllegalArgumentException(
                    "La sala " + id + " tiene funciones programadas: primero hay que eliminarlas");
        }
        salaRepository.deleteById(id);
    }
}
