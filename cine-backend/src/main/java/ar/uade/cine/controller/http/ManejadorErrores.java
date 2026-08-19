package ar.uade.cine.controller.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ar.uade.cine.dto.ErrorVistaDTO;
import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;
import ar.uade.cine.service.ventas.ButacaOcupadaException;

/**
 * La traducción de los errores del negocio a códigos HTTP, en un solo lugar.
 *
 * <p>Ninguna regla vive acá. Cuando un gestor rechaza algo lo hace con
 * IllegalArgumentException, y esta clase la convierte en un 400 con el mensaje
 * <strong>intacto</strong>, porque es el texto que el front le muestra al usuario. Que salga
 * sin tocar es parte del contrato con el frontend, y hay tests que lo comparan entero
 * justamente para que nadie lo reescriba de paso.
 *
 * <p>{@code @RestControllerAdvice} es lo que reemplazó al {@code registrarErrores} que tenía
 * el servidor Javalin: en vez de colgar manejadores de una instancia al armarla, se declaran
 * como métodos y valen para todos los controladores a la vez, incluidos los que todavía no
 * existen. Es la misma idea que había atrás de tener un solo lugar donde se armaba la API:
 * un controlador nuevo no puede quedarse sin la traducción de errores.
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

    /**
     * Perder una carrera por la última butaca no es una falla: es el resultado legítimo de
     * que otro haya confirmado primero. Por eso 409 y no 500, y el mensaje se muestra tal
     * cual —el front vuelve a pedir el mapa y repinta.
     */
    @ExceptionHandler(ButacaOcupadaException.class)
    public ResponseEntity<ErrorVistaDTO> butacaOcupada(ButacaOcupadaException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Un id que no es un número. Spring lo levanta al convertir el {@code @PathVariable}, y
     * se responde 404 y no 400 porque es lo mismo que pedir un recurso que no existe: la
     * URL no apunta a nada. Es el mismo mensaje que daba el {@code Parseo.id} de Javalin.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorVistaDTO> identificadorInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.NOT_FOUND, "El identificador " + e.getValue() + " no es válido");
    }

    /** Un cuerpo que no es JSON, o que se cortó a la mitad. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorVistaDTO> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo del pedido no es un JSON válido");
    }

    /** Una URL que no atiende ningún controlador. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorVistaDTO> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "No existe la ruta /" + e.getResourcePath());
    }

    /**
     * La base caída o una consulta rota no son culpa de quien llama, y el detalle interno no
     * le sirve: va al log del servidor, no a la respuesta.
     *
     * <p>{@link DataAccessException} es la jerarquía de Spring, y es lo que reemplazó a la
     * {@code PersistenciaException} propia: Spring Data traduce a ella lo que levantan
     * Hibernate y el driver, así que un mismo manejador cubre el timeout de conexión, la
     * consulta rota y la restricción violada sin que esta capa sepa qué motor hay abajo.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorVistaDTO> falloDePersistencia(DataAccessException e) {
        LOG.error("Falló el acceso a los datos", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo acceder a los datos");
    }

    /** Un comprobante que no se pudo escribir a disco. Mismo criterio: el detalle al log. */
    @ExceptionHandler(ComprobanteException.class)
    public ResponseEntity<ErrorVistaDTO> falloDeComprobante(ComprobanteException e) {
        LOG.error("Falló la emisión de un comprobante", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir el comprobante");
    }

    private static ResponseEntity<ErrorVistaDTO> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorVistaDTO(mensaje));
    }
}
