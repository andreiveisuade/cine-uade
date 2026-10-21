package ar.uade.cine.service.salas;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return agregar(nombre, tipo, butacasPorFila, Map.of(), Sala.LIMPIEZA_POR_DEFECTO);
    }

    // Una butaca especial por código, para cargar una sala a mano: así no hay dos listas que la repitan.
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<String, TipoAsiento> especiales) {
        Map<TipoAsiento, List<String>> porTipo = especiales.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        () -> new EnumMap<>(TipoAsiento.class),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        return agregar(nombre, tipo, butacasPorFila, porTipo, Sala.LIMPIEZA_POR_DEFECTO);
    }

    // Sin limpieza: el alta no la exige, a diferencia del nombre, el tipo y las filas.
    public Sala agregar(String nombre, TipoSala tipo, List<Integer> butacasPorFila,
                        Map<TipoAsiento, List<String>> especiales, Integer minutosLimpieza) {
        int limpieza = minutosLimpieza == null ? Sala.LIMPIEZA_POR_DEFECTO : minutosLimpieza;
        Sala sala = new Sala(nombre, tipo, limpieza);
        List<Asiento> asientos = sala.generarAsientos(butacasPorFila, especiales);
        if (salaRepository.existsByNombreIgnoreCase(sala.getNombre())) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }

        salaRepository.save(sala);
        asientoRepository.saveAll(asientos);
        return sala;
    }

    // El nombre repetido se busca sin la sala misma: la collation de MySQL ignora acentos y daba 409 falso
    // al renombrar "Sala Unica" a "Sala Única".
    // El tipo no cambia con funciones: una función 3D quedaría en una sala que no la proyecta.
    public Sala editar(int id, String nombre, TipoSala tipo, Integer minutosLimpieza) {
        Sala sala = salaRepository.exigir(id, "la sala");
        TipoSala tipoAnterior = sala.getTipo();
        sala.editar(nombre, tipo, minutosLimpieza == null ? sala.getMinutosLimpieza() : minutosLimpieza);
        if (salaRepository.existsByNombreIgnoreCaseAndIdNot(sala.getNombre(), id)) {
            throw new ConflictoDeNegocio("Ya existe una sala con ese nombre");
        }
        if (tipo != tipoAnterior && funcionRepository.existsBySala_Id(id)) {
            throw new DatoInvalido(
                    "La sala " + id + " tiene funciones programadas: no se le puede cambiar el tipo");
        }
        return sala;
    }

    // Devuelve la sala: asiento.getSala() es LAZY y fuera de la transacción no se podría leer.
    public Sala marcarFueraDeServicio(int salaId, String codigo) {
        Sala sala = salaRepository.exigir(salaId, "la sala");
        butaca(sala, codigo).marcarFueraDeServicio();
        return sala;
    }

    public Sala reponer(int salaId, String codigo) {
        Sala sala = salaRepository.exigir(salaId, "la sala");
        butaca(sala, codigo).reponer();
        return sala;
    }

    // 404 y no 400: la butaca viene en la ruta; las del cuerpo de una reserva siguen siendo 400.
    private Asiento butaca(Sala sala, String codigo) {
        return Asiento.conCodigo(asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(sala.getId()), codigo)
                .orElseThrow(() -> new RecursoNoEncontrado(
                        "No existe la butaca " + Asiento.normalizarCodigo(codigo)));
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
    public Sala obtener(int id) {
        return salaRepository.exigir(id, "la sala");
    }

    public void eliminar(int id) {
        Sala sala = salaRepository.exigir(id, "la sala");
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
        salaRepository.delete(sala);
    }
}
