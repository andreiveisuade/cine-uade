package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Borderó e informe por función y declaración jurada del INCAA; lee con consultas batch y los records suman.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GestorInformes {

    private final FuncionRepository funcionRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final CompraCandyRepository compraCandyRepository;
    private final Reloj reloj;

    public Bordero borderoDe(int funcionId) {
        return borderoDe(buscarFuncion(funcionId), reservaRepository.findByFuncion_Id(funcionId));
    }

    private Bordero borderoDe(Funcion funcion, List<Reserva> reservas) {
        return Bordero.de(funcion, reloj.ahora(), reservas, pagosPorReserva(reservas));
    }

    // Por fecha de la función y no del cobro: el INCAA declara espectadores de lo exhibido en la semana.
    public DeclaracionJurada declaracionJurada(LocalDate desde, LocalDate hasta) {
        PeriodoDeclarado periodo = PeriodoDeclarado.de(desde, hasta, reloj.hoy());

        List<Reserva> cobradas = reservaRepository.findCobradasDeFuncionesEntre(periodo.inicio(), periodo.fin());
        Map<Integer, Pago> pagos = pagosPorReserva(cobradas);
        Map<Integer, List<Reserva>> porFuncion = new LinkedHashMap<>();
        for (Reserva reserva : cobradas) {
            porFuncion.computeIfAbsent(reserva.getFuncionId(), id -> new ArrayList<>()).add(reserva);
        }

        List<DeclaracionJurada.FilaFuncion> filas = new ArrayList<>();
        for (List<Reserva> reservas : porFuncion.values()) {
            Funcion funcion = reservas.get(0).getFuncion();
            Pelicula pelicula = funcion.getPelicula();
            Bordero bordero = Bordero.de(funcion, reloj.ahora(), reservas, pagos);
            filas.add(new DeclaracionJurada.FilaFuncion(pelicula.getId(), bordero, funcion.getVersion(),
                    funcion.getProyeccion(), pelicula.getClasificacion()));
        }
        return DeclaracionJurada.de(periodo, reloj.ahora(), filas);
    }

    private Map<Integer, Pago> pagosPorReserva(List<Reserva> reservas) {
        Map<Integer, Pago> pagos = new HashMap<>();
        for (Pago cobro : pagoRepository.findByReservaIdIn(reservas.stream().map(Reserva::getId).toList())) {
            pagos.put(cobro.getReservaId(), cobro);
        }
        return pagos;
    }

    // Solo el candy con reservaId: el de mostrador va al arqueo (GestorCaja#arqueoCandyDe).
    public InformeFuncion informeDe(int funcionId) {
        Funcion funcion = buscarFuncion(funcionId);
        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
        List<CompraCandy> compras = compraCandyRepository.findByReservaIdIn(
                reservas.stream().map(Reserva::getId).toList());
        return InformeFuncion.de(borderoDe(funcion, reservas), compras);
    }

    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + funcionId));
    }
}
