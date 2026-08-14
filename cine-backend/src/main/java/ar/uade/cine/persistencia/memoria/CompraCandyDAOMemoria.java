package ar.uade.cine.persistencia.memoria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.persistencia.CompraCandyDAO;

/** Implementación en memoria: sirve para probar la lógica sin levantar MySQL. */
public class CompraCandyDAOMemoria implements CompraCandyDAO {

    private final List<CompraCandy> compras = new ArrayList<>();
    private int proximoId = 1;

    @Override
    public void guardar(CompraCandy compra) {
        compra.setId(proximoId++);
        compras.add(compra);
    }

    @Override
    public Optional<CompraCandy> buscarPorId(int id) {
        return compras.stream().filter(c -> c.getId() == id).findFirst();
    }

    @Override
    public List<CompraCandy> listarPorFecha(LocalDate fecha) {
        return compras.stream().filter(c -> c.getFecha().toLocalDate().equals(fecha)).toList();
    }

    /**
     * Compara con equals y no con ==: las dos claves son Integer porque admiten null, y
     * desreferenciarlas para comparar contra un int reventaba con la primera venta de
     * mostrador guardada, que es justo la que no tiene cliente.
     */
    @Override
    public List<CompraCandy> listarPorCliente(int clienteId) {
        return compras.stream().filter(c -> Integer.valueOf(clienteId).equals(c.getClienteId())).toList();
    }

    @Override
    public List<CompraCandy> listarPorReserva(int reservaId) {
        return compras.stream().filter(c -> Integer.valueOf(reservaId).equals(c.getReservaId())).toList();
    }
}
