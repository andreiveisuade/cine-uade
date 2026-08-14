package ar.uade.cine.dominio.candy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Igual que PeliculaImpl: el constructor corto arma la compra al venderla (sin id
 * todavía); el que recibe id es el que usa el DAO al reconstruirla desde la base.
 */
public class CompraCandyImpl implements CompraCandy {

    private int id;
    private final Integer clienteId;
    private final Integer reservaId;
    private final LocalDateTime fecha;
    private final MedioPago medio;
    private final String codigoAutorizacion;
    private final List<ItemCompra> items = new ArrayList<>();

    public CompraCandyImpl(Integer clienteId, Integer reservaId, LocalDateTime fecha, MedioPago medio,
                           String codigoAutorizacion, List<ItemCompra> items) {
        this.clienteId = clienteId;
        this.reservaId = reservaId;
        this.fecha = fecha;
        this.medio = medio;
        this.codigoAutorizacion = codigoAutorizacion;
        this.items.addAll(items);
    }

    public CompraCandyImpl(int id, Integer clienteId, Integer reservaId, LocalDateTime fecha,
                           MedioPago medio, String codigoAutorizacion, List<ItemCompra> items) {
        this(clienteId, reservaId, fecha, medio, codigoAutorizacion, items);
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Integer getClienteId() {
        return clienteId;
    }

    @Override
    public Integer getReservaId() {
        return reservaId;
    }

    @Override
    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public MedioPago getMedio() {
        return medio;
    }

    @Override
    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    @Override
    public List<ItemCompra> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public Dinero getTotal() {
        return Dinero.sumar(items.stream().map(ItemCompra::getSubtotal).toList());
    }

    @Override
    public String toString() {
        return "[" + id + "] cliente " + clienteId + " - " + items + " - $" + getTotal()
                + " - " + medio + " - " + fecha;
    }
}
