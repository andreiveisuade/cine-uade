package ar.uade.cine.service.ventas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ar.uade.cine.dto.ventas.ResultadoSincronizacionDTO;
import ar.uade.cine.dto.ventas.VentaTerminalDTO;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.ReservaRepository;

@Service
public class GestorSincronizacionTerminal {

    private final RestTemplate restTemplate;
    private final ReservaRepository reservaRepository;
    private final String urlServidor;

    public GestorSincronizacionTerminal(
            ReservaRepository reservaRepository,
            @Value("${servidor.central.url:http://localhost:8080}") String urlServidor) {
        this.reservaRepository = reservaRepository;
        this.restTemplate = new RestTemplate();
        this.urlServidor = urlServidor;
    }

    public ResultadoSincronizacionDTO sincronizarVenta(VentaTerminalDTO venta, Reserva reservaLocal) {
        try {
            ResultadoSincronizacionDTO resultado = restTemplate.postForObject(
                    urlServidor + "/api/sincronizacion/ventas",
                    venta,
                    ResultadoSincronizacionDTO.class
            );

            if (resultado != null && resultado.aceptada()) {
                reservaLocal.marcarSincronizada();
            } else if (resultado != null) {
                reservaLocal.marcarRechazada(resultado.motivoRechazo());
            }
            reservaRepository.save(reservaLocal);
            return resultado;
        } catch (Exception e) {
            reservaLocal.marcarPendiente();
            reservaRepository.save(reservaLocal);
            return new ResultadoSincronizacionDTO(venta.idLocal(), false, "Falla de red: " + e.getMessage());
        }
    }
}