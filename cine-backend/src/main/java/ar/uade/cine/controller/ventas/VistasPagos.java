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
import ar.uade.cine.dto.ventas.CotizacionVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.Cotizacion;

// Arma los JSON de pagos, checkouts, cotizaciones y los pagos del arqueo; Assembler con Cartelera y Usuarios.
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

    // El arqueo dice qué se vendió y a quién; una consulta para todos los pagos (VistasVentasTest)
    public List<PagoVistaDTO> pagosDeArqueo(List<Pago> lista) {
        Map<Integer, Reserva> porId = reservas.conDetalle(lista.stream().map(Pago::getReservaId).toList())
                .stream()
                .collect(Collectors.toMap(Reserva::getId, Function.identity()));
        return lista.stream()
                .map(p -> {
                    Reserva reserva = porId.get(p.getReservaId());
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

    public CotizacionVistaDTO cotizacion(Cotizacion c) {
        return new CotizacionVistaDTO(c.medio().name(), c.entradas().aPesos(), c.descuento().monto().aPesos(),
                c.descuento().promocionId(), c.descuento().promocion(), c.candy().aPesos(), c.total().aPesos());
    }

    public CheckoutVistaDTO checkout(PasarelaPagos.Checkout c) {
        return new CheckoutVistaDTO(c.id(), c.reservaId(), c.medio().name(), c.monto().aPesos(),
                c.urlPago(), c.codigoQr());
    }
}
