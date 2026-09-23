package ar.uade.cine.service.promociones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.PromocionMontoFijo;
import ar.uade.cine.model.promociones.PromocionNxM;
import ar.uade.cine.model.promociones.PromocionPorcentaje;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.PromocionRepository;
import ar.uade.cine.model.dinero.Dinero;

@Service
@Transactional
public class GestorPromociones implements PoliticaPromociones {

    private final PromocionRepository promocionRepository;

    public GestorPromociones(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    public Promocion crearPorcentaje(String nombre, double porcentaje, CondicionesPromocion condiciones) {
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new IllegalArgumentException("El porcentaje tiene que estar entre 1 y 99");
        }
        return guardar(new PromocionPorcentaje(nombre, porcentaje, condiciones.desde(), condiciones.hasta(),
                condiciones.dias(), condiciones.horaDesde(), condiciones.horaHasta(),
                condiciones.mediosPago()), nombre, condiciones);
    }

    public Promocion crearMontoFijo(String nombre, Dinero monto, CondicionesPromocion condiciones) {
        if (monto == null || !monto.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El monto del descuento debe ser mayor a cero");
        }
        return guardar(new PromocionMontoFijo(nombre, monto, condiciones.desde(), condiciones.hasta(),
                condiciones.dias(), condiciones.horaDesde(), condiciones.horaHasta(),
                condiciones.mediosPago()), nombre, condiciones);
    }

    public Promocion crearNxM(String nombre, int lleva, int paga, CondicionesPromocion condiciones) {
        // Un 2x2 no descuenta y un 2x3 cobraría de más.
        if (lleva <= paga || paga <= 0) {
            throw new IllegalArgumentException("En un NxM hay que llevar más de lo que se paga");
        }
        return guardar(new PromocionNxM(nombre, lleva, paga, condiciones.desde(), condiciones.hasta(),
                condiciones.dias(), condiciones.horaDesde(), condiciones.horaHasta(),
                condiciones.mediosPago()), nombre, condiciones);
    }

    private Promocion guardar(Promocion promocion, String nombre, CondicionesPromocion condiciones) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La promoción necesita un nombre");
        }
        LocalDate desde = condiciones.desde();
        LocalDate hasta = condiciones.hasta();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La vigencia tiene que empezar antes de terminar");
        }
        if (promocionRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("Ya hay una promoción llamada " + nombre);
        }
        promocionRepository.save(promocion);
        return promocion;
    }

    // R16: solo participan las entradas generales. Empate: gana la de menor id, para que el cobro sea determinístico.
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
                .filter(e -> e.tarifa() == TipoTarifa.GENERAL)
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

    public void desactivar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.setActiva(false);
        promocionRepository.save(promocion);
    }

    public void activar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.setActiva(true);
        promocionRepository.save(promocion);
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
                .orElseThrow(() -> new IllegalArgumentException("No existe la promoción " + id));
    }
}
