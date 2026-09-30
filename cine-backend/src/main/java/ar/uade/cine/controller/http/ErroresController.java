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
// Lo que rechaza el firewall de Spring Security (una ruta con ;, // o %2e) y lo que escapa de un filtro no
// llegan a ningún controller: el servidor lo reenvía a /error. El de Boot contestaba
// {"timestamp","status","error":"Bad Request","path"}, en inglés, y Swing y la web mostraban «Bad Request».
// Con un ErrorController declarado, Boot no registra el suyo. Oculto en Swagger: no es una ruta de la API.
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
