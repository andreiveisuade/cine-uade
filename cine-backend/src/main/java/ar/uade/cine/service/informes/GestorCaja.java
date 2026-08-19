package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.PagoRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.model.dinero.Dinero;

/**
 * El cierre de caja de un día: cuánto entró, por qué medio, y por cuál de las dos cajas
 * del cine —la boletería y la barra del candy—.
 *
 * <p>Es el informe que se corta <strong>por día</strong>, al lado de {@link GestorInformes}
 * que corta <strong>por función</strong>. Los dos son lecturas y ninguno escribe nada del
 * negocio, por eso viven en el mismo paquete.
 *
 * <p><strong>Por qué existe:</strong> antes el arqueo estaba repartido en los gestores que
 * cobran. El de boletería vivía en {@code GestorPagos} y el de candy en
 * {@code GestorCandy}, así que "cuánto entró hoy" no se podía contestar sin preguntarle a
 * dos gestores transaccionales y sumar afuera. Eran dos razones de cambio metidas en la
 * misma clase: cobrar cambia cuando cambia cómo se cobra, y el arqueo cuando cambia qué se
 * declara. Sacarlo de ahí deja a {@code GestorPagos} haciendo una sola cosa —cobrar— y
 * pone las dos cajas juntas, que es como se leen.
 *
 * <p>No cobra ni corrige nada: solo lee. Si un número no cierra, la respuesta está en los
 * pagos, no acá.
 */
@Service
public class GestorCaja {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final CompraCandyRepository compraCandyRepository;

    public GestorCaja(PagoRepository pagoRepository, ReservaRepository reservaRepository, CompraCandyRepository compraCandyRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.compraCandyRepository = compraCandyRepository;
    }

    /**
     * El cierre de la boletería del día: el total, cuántas entradas se vendieron y cuánto
     * entró por cada medio de pago.
     *
     * <p>Se recorre una sola vez la lista de cobros del día: los tres números salen de la
     * misma pasada.
     */
    public Arqueo arqueoDe(LocalDate fecha) {
        List<Pago> delDia = pagoRepository.findByDia(fecha);

        Map<MedioPago, Arqueo.TotalPorMedio> porMedio = new EnumMap<>(MedioPago.class);
        Dinero total = Dinero.CERO;
        int entradas = 0;
        for (Pago pago : delDia) {
            Arqueo.TotalPorMedio acumulado = porMedio.getOrDefault(pago.getMedio(),
                    new Arqueo.TotalPorMedio(0, Dinero.CERO));
            porMedio.put(pago.getMedio(), new Arqueo.TotalPorMedio(acumulado.cantidad() + 1,
                    acumulado.total().mas(pago.getMonto())));
            total = total.mas(pago.getMonto());
            entradas += entradasDe(pago);
        }
        return new Arqueo(fecha, total, entradas, porMedio, delDia);
    }

    /** Cuánto se cobró en boletería en el día, sin el desglose. */
    public Dinero totalCobrado(LocalDate fecha) {
        return Dinero.sumar(pagoRepository.findByDia(fecha).stream().map(Pago::getMonto).toList());
    }

    public List<Pago> listarDelDia(LocalDate fecha) {
        return pagoRepository.findByDia(fecha);
    }

    /**
     * Cuánto entró por el candy en el día, mostrador incluido.
     *
     * <p>Es la otra mitad de la caja y va aparte del arqueo de boletería a propósito: son
     * dos circuitos con dos comprobantes distintos, y el borderó que se le declara al INCAA
     * solo mira el de entradas. Sumarlos en un número único obligaría a volver a separarlos
     * para declarar.
     */
    public Dinero totalCandyDe(LocalDate fecha) {
        return Dinero.sumar(compraCandyRepository.findByDia(fecha).stream()
                .map(CompraCandy::getTotal).toList());
    }

    /**
     * Cuántas butacas se llevó ese cobro. El pago no lo guarda —sería el mismo dato en dos
     * lados— así que se cuenta sobre las entradas de su reserva.
     */
    private int entradasDe(Pago pago) {
        return reservaRepository.findById(pago.getReservaId())
                .map(Reserva::getCantidadEntradas)
                .orElse(0);
    }
}
