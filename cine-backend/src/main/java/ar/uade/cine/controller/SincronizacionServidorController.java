package ar.uade.cine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.ventas.DatosBajadaTerminalDTO;
import ar.uade.cine.dto.ventas.ResultadoSincronizacionDTO;
import ar.uade.cine.dto.ventas.VentaTerminalDTO;
import ar.uade.cine.service.ventas.GestorSincronizacionServidor;

@RestController
@RequestMapping("/api/sincronizacion")
public class SincronizacionServidorController {

    private final GestorSincronizacionServidor gestorSincronizacion;

    // Inyección de dependencias por constructor
    public SincronizacionServidorController(GestorSincronizacionServidor gestorSincronizacion) {
        this.gestorSincronizacion = gestorSincronizacion;
    }

    @GetMapping("/bajada")
    public DatosBajadaTerminalDTO descargarDatos() {
        return gestorSincronizacion.prepararBajada();
    }

    @PostMapping("/ventas")
    @ResponseStatus(HttpStatus.OK)
    public ResultadoSincronizacionDTO recibirVenta(@RequestBody VentaTerminalDTO venta) {
        return gestorSincronizacion.procesarVentaTerminal(venta);
    }
}