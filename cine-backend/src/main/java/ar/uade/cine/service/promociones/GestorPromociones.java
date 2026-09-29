package ar.uade.cine.service.promociones;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.PromocionMontoFijo;
import ar.uade.cine.model.promociones.PromocionNxM;
import ar.uade.cine.model.promociones.PromocionPorcentaje;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.repository.promociones.PromocionRepository;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Alta, activación y mejor promoción al cobrar (R15, R16); implementa PoliticaPromociones.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorPromociones implements PoliticaPromociones {

    private final PromocionRepository promocionRepository;

    // Los valores llegan en objeto desde el pedido: que falte el propio del tipo lo dice el gestor,
    // porque la entidad los recibe primitivos y no tiene cómo enterarse. El resto de las reglas
    // (rango, NxM, vigencia, nombre) las valida cada clase de Promocion al construirse.
    public Promocion crearPorcentaje(String nombre, Double porcentaje, CondicionesPromocion condiciones) {
        TipoPromocion.PORCENTAJE.exigirCampos(porcentaje);
        return guardar(new PromocionPorcentaje(nombre, porcentaje, condiciones));
    }

    public Promocion crearMontoFijo(String nombre, Dinero monto, CondicionesPromocion condiciones) {
        TipoPromocion.MONTO_FIJO.exigirCampos(monto);
        return guardar(new PromocionMontoFijo(nombre, monto, condiciones));
    }

    public Promocion crearNxM(String nombre, Integer lleva, Integer paga, CondicionesPromocion condiciones) {
        TipoPromocion.NXM.exigirCampos(lleva, paga);
        return guardar(new PromocionNxM(nombre, lleva, paga, condiciones));
    }

    // El nombre repetido es lo único que la entidad no puede ver sola: hace falta el repositorio.
    // La entidad ya lo recortó, así que se busca lo mismo que se va a guardar.
    private Promocion guardar(Promocion promocion) {
        String nombre = promocion.getNombre();
        if (promocionRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoDeNegocio("Ya existe una promoción con ese nombre");
        }
        promocionRepository.save(promocion);
        return promocion;
    }

    // R16: solo participan las tarifas que lo dicen (TipoTarifa#participaDePromociones).
    // Empate: gana la de menor id, para que el cobro sea determinístico.
    @Transactional(readOnly = true)
    @Override
    public Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                  MedioPago medio) {
        return mejorDescuento(entradas, inicioFuncion, medio)
                .map(c -> new Descuento(c.promocion().getId(), c.monto()))
                .orElseGet(Descuento::ninguno);
    }

    private record Candidata(Promocion promocion, Dinero monto) {
    }

    private Optional<Candidata> mejorDescuento(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                               MedioPago medio) {
        List<Entrada> alcanzadas = entradas.stream()
                .filter(e -> e.tarifa().participaDePromociones())
                .toList();
        if (alcanzadas.isEmpty()) {
            return Optional.empty();
        }
        return promocionRepository.findByActivaTrue().stream()
                .filter(p -> p.aplicaA(inicioFuncion, medio))
                .map(p -> new Candidata(p, p.calcularDescuento(alcanzadas)))
                .filter(c -> c.monto().esMayorQue(Dinero.CERO))
                .max(Comparator.comparing(Candidata::monto)
                        .thenComparing(c -> c.promocion().getId(), Comparator.reverseOrder()));
    }

    public Promocion desactivar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.desactivar();
        return promocion;
    }

    public Promocion activar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.activar();
        return promocion;
    }

    @Transactional(readOnly = true)
    public List<Promocion> listar() {
        return promocionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Promocion> buscar(int id) {
        return promocionRepository.findById(id);
    }

    private Promocion buscarOFallar(int id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la promoción " + id));
    }
}
