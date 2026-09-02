package ar.uade.cine.service.ventas;

import org.springframework.stereotype.Service;
import ar.uade.cine.dto.ventas.VentaTerminalDTO;
import ar.uade.cine.dto.ventas.ResultadoSincronizacionDTO;
import ar.uade.cine.dto.ventas.DatosBajadaTerminalDTO;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.controller.vistas.VistasCartelera;
import ar.uade.cine.controller.vistas.VistasSalas;
import ar.uade.cine.controller.vistas.VistasVentas;

@Service
public class GestorSincronizacionServidor {

    private final GestorReservas gestorReservas;
    private final GestorPagos gestorPagos;
    private final GestorClientes gestorClientes;
    private final GestorCartelera gestorCartelera;
    private final GestorSalas gestorSalas;
    private final GestorFunciones gestorFunciones;
    private final VistasCartelera vistasCartelera;
    private final VistasSalas vistasSalas;
    private final VistasVentas vistasVentas;

    public GestorSincronizacionServidor(
            GestorReservas gestorReservas,
            GestorPagos gestorPagos,
            GestorClientes gestorClientes,
            GestorCartelera gestorCartelera,
            GestorSalas gestorSalas,
            GestorFunciones gestorFunciones,
            VistasCartelera vistasCartelera,
            VistasSalas vistasSalas,
            VistasVentas vistasVentas) {
        this.gestorReservas = gestorReservas;
        this.gestorPagos = gestorPagos;
        this.gestorClientes = gestorClientes;
        this.gestorCartelera = gestorCartelera;
        this.gestorSalas = gestorSalas;
        this.gestorFunciones = gestorFunciones;
        this.vistasCartelera = vistasCartelera;
        this.vistasSalas = vistasSalas;
        this.vistasVentas = vistasVentas;
    }

    public DatosBajadaTerminalDTO prepararBajada() {
        var peliculas = gestorCartelera.listarEnCartelera().stream().map(vistasCartelera::pelicula).toList();
        var salas = gestorSalas.listar().stream().map(vistasSalas::sala).toList();
        var funciones = gestorFunciones.listar().stream().map(vistasCartelera::funcionConPelicula).toList();
        var reservas = vistasVentas.reservas(gestorReservas.listarActivas());

        return new DatosBajadaTerminalDTO(peliculas, salas, funciones, reservas);
    }

    public ResultadoSincronizacionDTO procesarVentaTerminal(VentaTerminalDTO venta) {
        try {
            Cliente cliente = gestorClientes.identificar(venta.nombreCliente(), venta.emailCliente());
            
            Reserva reserva = gestorReservas.reservar(
                    venta.funcionId(),
                    cliente.getId(),
                    venta.butacas(),
                    null
            );

            MedioPago medio = Parseo.constante(MedioPago.class, venta.medioPago(), "el medio de pago");
            gestorPagos.cobrar(reserva.getId(), medio, venta.codigoAutorizacion());

            return new ResultadoSincronizacionDTO(venta.idLocal(), true, null);
        } catch (ButacaOcupadaException e) {
            return new ResultadoSincronizacionDTO(venta.idLocal(), false, "Conflicto: " + e.getMessage());
        } catch (Exception e) {
            return new ResultadoSincronizacionDTO(venta.idLocal(), false, "Rechazo de negocio: " + e.getMessage());
        }
    }
}