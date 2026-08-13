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
}
