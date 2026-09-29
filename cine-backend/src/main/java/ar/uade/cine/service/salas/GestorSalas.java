package ar.uade.cine.service.salas;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.programaciones.ProgramacionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// ABM de salas y sus butacas (R9, R12); la sala valida y crea sus butacas, el gestor consulta la base.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorSalas {

    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final FuncionRepository funcionRepository;
    private final ProgramacionRepository programacionRepository;

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila) {
        return agregar(nombre, tipo, butacasPorFila, Map.of());
    }

    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales) {
        return agregar(nombre, tipo, butacasPorFila, especiales, Sala.LIMPIEZA_POR_DEFECTO);
    }

    // Sin limpieza, la de siempre: el alta no la exige, a diferencia del nombre, el tipo y las filas.
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales, Integer minutosLimpieza) {
        int limpieza = minutosLimpieza == null ? Sala.LIMPIEZA_POR_DEFECTO : minutosLimpieza;
        // Los datos de la sala y sus filas se rechazan antes de consultar el nombre repetido.
        Sala sala = new Sala(nombre, tipo, limpieza);
        List<Asiento> asientos = sala.generarAsientos(butacasPorFila, especiales);
        if (salaRepository.existsByNombreIgnoreCase(sala.getNombre())) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }

        salaRepository.save(sala);
        asientoRepository.saveAll(asientos);
        return sala;
    }

    // El tipo no cambia con funciones: una función 3D quedaría en una sala que no la proyecta.
    public Sala editar(int id, String nombre, TipoSala tipo, Integer minutosLimpieza) {
        Sala sala = buscarOFallar(id);
        int limpieza = minutosLimpieza == null ? sala.getMinutosLimpieza() : minutosLimpieza;
        String nuevoNombre = Sala.normalizarNombre(nombre);
        // Los chequeos contra la base van antes de editar: con la sala ya modificada, la consulta
        // del nombre repetido haría flush y se encontraría a sí misma.
        if (!sala.getNombre().equalsIgnoreCase(nuevoNombre)
                && salaRepository.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }
        if (tipo != null && tipo != sala.getTipo() && funcionRepository.existsBySala_Id(id)) {
            throw new DatoInvalido(
                    "La sala " + id + " tiene funciones programadas: no se le puede cambiar el tipo");
        }
        sala.editar(nombre, tipo, limpieza);
        return sala;
    }

    // Devuelve la sala y no la butaca: es lo que muestra quien la marcó, y asiento.getSala() es
    // LAZY, así que fuera de la transacción no se podría leer.
    public Sala marcarFueraDeServicio(int salaId, String codigo) {
        Sala sala = buscarOFallar(salaId);
        butaca(sala, codigo).marcarFueraDeServicio();
        return sala;
    }

    public Sala reponer(int salaId, String codigo) {
        Sala sala = buscarOFallar(salaId);
        butaca(sala, codigo).reponer();
        return sala;
    }

    // 404 y no 400: la butaca viene en la ruta, así que es el recurso que no existe. Las de una
    // reserva vienen en el cuerpo y siguen siendo un pedido inválido (Asiento.exigirConCodigo).
    private Asiento butaca(Sala sala, String codigo) {
        return Asiento.conCodigo(asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(sala.getId()), codigo)
                .orElseThrow(() -> new RecursoNoEncontrado(Asiento.inexistente(codigo)));
    }

    private Sala buscarOFallar(int id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la sala " + id));
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
            throw new DatoInvalido(
                    "La sala " + id + " tiene funciones programadas: primero hay que eliminarlas");
        }
        // Una grilla sin funciones generadas también la nombra, y chocaba con la FK en un 500.
        // Las grillas no se borran, solo se dan de baja: la sala queda.
        if (programacionRepository.existsBySala_Id(id)) {
            throw new DatoInvalido(
                    "La sala " + id + " está programada en una grilla: no se puede eliminar");
        }
        salaRepository.deleteById(id);
    }
}
