package ar.uade.cine.persistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.candy.CompraCandy;

/**
 * Sin actualizar: una venta de mostrador se emite y no se toca más. Corregirla sería
 * otra operación (una devolución), que este sistema no modela.
 */
public interface CompraCandyDAO {

    void guardar(CompraCandy compra);

    Optional<CompraCandy> buscarPorId(int id);

    /** Para el arqueo: cuánto entró por el candy en un día. */
    List<CompraCandy> listarPorFecha(LocalDate fecha);

    List<CompraCandy> listarPorCliente(int clienteId);

    /**
     * Las compras que se le agregaron a una reserva. Es lo único que permite atribuirle
     * candy a una función: la venta de mostrador no dice a qué función va quien la compra.
     */
    List<CompraCandy> listarPorReserva(int reservaId);
}
