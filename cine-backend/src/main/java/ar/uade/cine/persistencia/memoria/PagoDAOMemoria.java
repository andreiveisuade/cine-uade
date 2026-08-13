package ar.uade.cine.persistencia.memoria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.persistencia.PagoDAO;

public class PagoDAOMemoria implements PagoDAO {

    private final List<Pago> pagos = new ArrayList<>();
    private int proximoId = 1;

    @Override
    public void guardar(Pago pago) {
        pago.setId(proximoId++);
        pagos.add(pago);
    }

    @Override
    public Optional<Pago> buscarPorReserva(int reservaId) {
        return pagos.stream().filter(p -> p.getReservaId() == reservaId).findFirst();
    }

    @Override
    public List<Pago> listarPorFecha(LocalDate fecha) {
        return pagos.stream().filter(p -> p.getFecha().toLocalDate().equals(fecha)).toList();
    }

    @Override
    public List<Pago> listar() {
        return new ArrayList<>(pagos);
    }
}
