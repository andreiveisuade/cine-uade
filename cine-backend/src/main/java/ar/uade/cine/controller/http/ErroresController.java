package ar.uade.cine.controller.http;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.comun.ErrorVistaDTO;

import io.swagger.v3.oas.annotations.Hidden;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

// Contesta /error, adonde llega lo que no pasó por ManejadorErrores; {error} en castellano según el status.
// Lo que rechaza el firewall de Spring Security o escapa de un filtro no llega a ningún controller: se reenvía acá.
// Declarar un ErrorController evita el de Boot (JSON en inglés); oculto en Swagger.
@Hidden
@RestController
public class ErroresController implements ErrorController {

    // Sin status es que alguien pidió /error directo: el de Boot contestaba 500, y este también.
    @RequestMapping("/error")
    public ResponseEntity<ErrorVistaDTO> error(HttpServletRequest pedido) {
        Object codigo = pedido.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatusCode estado = codigo instanceof Integer numero
                ? HttpStatusCode.valueOf(numero)
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ManejadorErrores.responder(estado, ManejadorErrores.textoDe(estado));
    }
}
