package ar.uade.cine.swing.api.dto.ventas;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;

import java.util.List;

// Una reserva como la ven el encargado y la Puerta; cobrable y cancelable los decide el backend.
// candy: lo que no se anuló; con la reserva sin cobrar, es lo pedido en la web, que se cobra con ella (R30).
// venceEn: hasta cuándo se puede pagar; es lo que importa mientras está reservada.
public record Reserva(int id, String estado, String codigo, String ingresadaEn, String venceEn,
                      List<Entrada> entradas, double total, Funcion funcion, Pelicula pelicula, Sala sala,
                      Cliente cliente, Pago pago, boolean cobrable, boolean cancelable, List<CompraCandy> candy) {

    public List<CompraCandy> candy() {
        return candy == null ? List.of() : candy;
    }

    // Suma de lo que el backend ya totalizó: el precio de cada renglón no se recalcula acá.
    public double totalCandy() {
        return candy().stream().mapToDouble(CompraCandy::total).sum();
    }
}
