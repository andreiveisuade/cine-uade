package ar.uade.cine.controller.http;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ar.uade.cine.dto.ErrorVistaDTO;
import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;
import ar.uade.cine.service.usuarios.CredencialesInvalidas;
import ar.uade.cine.service.ventas.ButacaOcupadaException;

/**
 * Traduce los errores del negocio a códigos HTTP. Un {@code IllegalArgumentException} de un
 * gestor sale como 400 con el mensaje <strong>intacto</strong>: es el texto que el front le
 * muestra al usuario, y hay tests que lo comparan entero.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Lo que rechaza un gestor: dato inválido o regla de negocio incumplida. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorVistaDTO> datoInvalido(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoEncontrado.class)
    public ResponseEntity<ErrorVistaDTO> noEncontrado(NoEncontrado e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** El pedido está bien formado pero quien llama no es quien dice ser. */
    @ExceptionHandler(CredencialesInvalidas.class)
    public ResponseEntity<ErrorVistaDTO> credencialesInvalidas(CredencialesInvalidas e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Perder la carrera por una butaca es legítimo, no una falla: 409 y el front repinta el mapa. */
    @ExceptionHandler(ButacaOcupadaException.class)
    public ResponseEntity<ErrorVistaDTO> butacaOcupada(ButacaOcupadaException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Un id que no es número: 404 y no 400, porque la URL no apunta a nada. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorVistaDTO> identificadorInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.NOT_FOUND, "El identificador " + e.getValue() + " no es válido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorVistaDTO> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo del pedido no es un JSON válido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorVistaDTO> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName());
    }

    /** 405 y no 404: la ruta existe, y {@code Allow} dice qué métodos acepta. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorVistaDTO> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        Set<HttpMethod> aceptados = e.getSupportedHttpMethods();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(aceptados == null ? new HttpMethod[0] : aceptados.toArray(HttpMethod[]::new))
                .body(new ErrorVistaDTO("La ruta no acepta " + e.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorVistaDTO> formatoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo del pedido tiene que ser JSON");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorVistaDTO> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "No existe la ruta /" + e.getResourcePath());
    }

    /** Una falla de la base no es culpa de quien llama: mensaje genérico y el detalle al log. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorVistaDTO> falloDePersistencia(DataAccessException e) {
        LOG.error("Falló el acceso a los datos", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo acceder a los datos");
    }

    @ExceptionHandler(ComprobanteException.class)
    public ResponseEntity<ErrorVistaDTO> falloDeComprobante(ComprobanteException e) {
        LOG.error("Falló la emisión de un comprobante", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir el comprobante");
    }

    /**
     * Sin esto Spring contesta con su propio formato y el front, que espera {@code {error}},
     * mostraría un mensaje vacío. Las de Spring MVC conservan su código (406, 413...).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVistaDTO> errorInesperado(Exception e) {
        if (e instanceof ErrorResponse deSpring) {
            return ResponseEntity.status(deSpring.getStatusCode())
                    .body(new ErrorVistaDTO(deSpring.getBody().getDetail()));
        }
        LOG.error("Error no previsto", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor");
    }

    private static ResponseEntity<ErrorVistaDTO> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorVistaDTO(mensaje));
    }
}
