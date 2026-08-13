package ar.uade.cine.persistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Pago;

public interface PagoDAO {

    void guardar(Pago pago);

    Optional<Pago> buscarPorReserva(int reservaId);

    /** Para el arqueo: cuánto entró en un día. */
    List<Pago> listarPorFecha(LocalDate fecha);

    List<Pago> listar();
}
