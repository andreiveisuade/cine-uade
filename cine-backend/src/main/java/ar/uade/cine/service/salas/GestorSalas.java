package ar.uade.cine.service.salas;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

// ABM de salas y sus butacas (R9, R12); la sala valida y crea sus butacas, el gestor consulta la base.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorSalas {

    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final FuncionRepository funcionRepository;

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        return agregar(nombre, tipo, butacasPorFila, Map.of());
    }

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales) {
        return agregar(nombre, tipo, butacasPorFila, especiales, Sala.LIMPIEZA_POR_DEFECTO);
    }

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales, int minutosLimpieza) {
        // Los datos de la sala y sus filas se rechazan antes de consultar el nombre repetido.
        Sala sala = new Sala(nombre, tipo, minutosLimpieza);
        List<Asiento> asientos = sala.generarAsientos(butacasPorFila, especiales);
        if (salaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }

        salaRepository.save(sala);
        asientoRepository.saveAll(asientos);
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

    public void marcarFueraDeServicio(int salaId, String codigo) {
        Asiento asiento = butaca(salaId, codigo);
        asiento.marcarFueraDeServicio();
        asientoRepository.save(asiento);
    }

    public void reponer(int salaId, String codigo) {
        Asiento asiento = butaca(salaId, codigo);
        asiento.reponer();
        asientoRepository.save(asiento);
    }

    private Asiento butaca(int salaId, String codigo) {
        return Asiento.conCodigo(asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(salaId), codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en la sala " + salaId));
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
