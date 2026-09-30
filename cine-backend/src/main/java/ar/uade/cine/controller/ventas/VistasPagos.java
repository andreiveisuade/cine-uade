package ar.uade.cine.controller.ventas;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.cartelera.VistasCartelera;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.usuarios.VistasUsuarios;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.ventas.CheckoutVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.ventas.ConsultasReservas;

// Arma los JSON de pagos, checkouts y los pagos del arqueo; Assembler que compone Cartelera y Usuarios.
// Aparte de VistasReservas porque lo usan otros: la caja del encargado y el checkout del cliente.
@Component
@RequiredArgsConstructor
public class VistasPagos {

    private final ConsultasReservas reservas;
    private final VistasCartelera vistasCartelera;
    private final VistasUsuarios vistasUsuarios;

    public PagoVistaDTO pago(Pago p) {
        return dto(p, null, null, null);
    }

    // El arqueo sí dice qué se vendió y a quién. Por el mismo camino que las reservas: una
    // consulta para todas, no tres por pago. VistasVentasTest cuenta las sentencias.
    public List<PagoVistaDTO> pagosDeArqueo(List<Pago> lista) {
        Map<Integer, Reserva> porId = reservas.conDetalle(lista.stream().map(Pago::getReservaId).toList())
                .stream()
                .collect(Collectors.toMap(Reserva::getId, Function.identity()));
        return lista.stream()
                .map(p -> {
                    Reserva reserva = porId.get(p.getReservaId());
                    if (reserva == null) {
                        return pago(p);
                    }
                    return dto(p, vistasCartelera.pelicula(reserva.getFuncion().getPelicula()),
                            vistasUsuarios.cliente(reserva.getCliente()), reserva.getCantidadEntradas());
                })
                .toList();
    }

    private static PagoVistaDTO dto(Pago p, PeliculaVistaDTO pelicula, ClienteVistaDTO cliente, Integer entradas) {
        return new PagoVistaDTO(p.getId(), p.getReservaId(), p.getSubtotal().aPesos(), p.getPromocionId(),
                p.getDescuento().aPesos(), p.getMonto().aPesos(), p.getMedio().name(),
                Fechas.texto(p.getFecha()), p.getCodigoAutorizacion(), pelicula, cliente, entradas);
    }

    public CheckoutVistaDTO checkout(PasarelaPagos.Checkout c) {
        return new CheckoutVistaDTO(c.id(), c.reservaId(), c.medio().name(), c.monto().aPesos(),
                c.urlPago(), c.codigoQr());
    }
}
