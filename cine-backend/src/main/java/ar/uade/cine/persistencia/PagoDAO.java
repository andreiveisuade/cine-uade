package ar.uade.cine.persistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Pago;

/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 * Sin actualizar ni eliminar: un pago, una vez cobrado, no se modifica. Corregirlo
 * es un circuito de devolución que este sistema no modela (ver R13).
 */
public interface PagoDAO {

    void guardar(Pago pago);

    Optional<Pago> buscarPorReserva(int reservaId);

    /** Para el arqueo: cuánto entró en un día. */
    List<Pago> listarPorFecha(LocalDate fecha);

    List<Pago> listar();
}
