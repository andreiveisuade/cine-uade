package ar.uade.cine.controller.http;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import ar.uade.cine.dto.comun.ErrorVistaDTO;
import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;
import ar.uade.cine.service.ventas.ButacaOcupadaException;
import ar.uade.cine.service.ConflictoDeNegocio;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.servlet.http.HttpServletRequest;

// Traduce cada excepción a status HTTP y JSON {error}; @RestControllerAdvice: ningún controller atrapa.
// Dentro de este advice Spring elige el handler de la excepción más cercana en la jerarquía
// (ExceptionDepthComparator), no el primero declarado: el orden de los métodos no cambia nada.
@RestControllerAdvice
@Slf4j
public class ManejadorErrores {

    // El mensaje sale intacto: es el texto que ve el usuario.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorVistaDTO> datoInvalido(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorVistaDTO> pedidoIncompleto(MethodArgumentNotValidException e) {
        return responder(HttpStatus.BAD_REQUEST, primerError(e));
    }

    // Por orden de declaración en el DTO: el validador no garantiza ninguno y el mensaje cambiaría entre corridas.
    private static String primerError(MethodArgumentNotValidException e) {
        Class<?> dto = e.getParameter().getParameterType();
        List<String> campos = dto.isRecord()
                ? Arrays.stream(dto.getRecordComponents()).map(RecordComponent::getName).toList()
                : List.of();
        return e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt((FieldError error) -> campos.indexOf(error.getField())))
                .map(FieldError::getDefaultMessage)
                .orElse("El pedido no es válido");
    }

    @ExceptionHandler(RecursoNoEncontrado.class)
    public ResponseEntity<ErrorVistaDTO> noEncontrado(RecursoNoEncontrado e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoDeNegocio.class)
    public ResponseEntity<ErrorVistaDTO> conflicto(ConflictoDeNegocio e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ButacaOcupadaException.class)
    public ResponseEntity<ErrorVistaDTO> butacaOcupada(ButacaOcupadaException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    // Dos operaciones sobre la misma reserva a la vez (cobrar y cancelar): la segunda escribe
    // sobre una versión vieja. Spring ya traduce la OptimisticLockException de JPA a esta, y
    // controller/ no puede nombrar jakarta.persistence (ArquitecturaTest). Aunque es subclase de
    // DataAccessException no cae en su 500: gana este handler por ser el más cercano.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorVistaDTO> conflictoDeVersion(OptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "La reserva cambió mientras se procesaba: volvé a intentarlo");
    }

    // Lo que queda después de las validaciones de los gestores son carreras: dos altas que pasan
    // el mismo existsBy… y la segunda choca con el UNIQUE, o dos cobros con UNIQUE(pago.reserva_id).
    // La base no falló, así que no es el 500 de DataAccessException. La causa, al log: nombra la
    // restricción, y eso no es para el usuario.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorVistaDTO> conflictoDeIntegridad(DataIntegrityViolationException e) {
        log.warn("Un pedido chocó con una restricción de la base: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT,
                "Otro pedido cambió estos datos al mismo tiempo: recargá y volvé a intentarlo");
    }

    // Solo las variables de ruta llegan tipadas (int id): la query viaja como String y la lee Parseo.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorVistaDTO> identificadorInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.NOT_FOUND, "El identificador " + e.getValue() + " no es válido");
    }

    // Un JSON bien formado con un tipo equivocado ("precio": "abc") no es "JSON inválido": el
    // mensaje nombra el campo, que es lo que el usuario puede corregir. Sin causa es que no vino
    // cuerpo: Spring lo lee como null y lo rechaza él, sin pasar por Jackson.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorVistaDTO> cuerpoIlegible(HttpMessageNotReadableException e) {
        if (e.getCause() == null) {
            return responder(HttpStatus.BAD_REQUEST, "Falta el cuerpo del pedido");
        }
        if (e.getCause() instanceof MismatchedInputException tipo && !tipo.getPath().isEmpty()) {
            String campo = tipo.getPath().stream()
                    .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                    .collect(Collectors.joining("."));
            String valor = tipo instanceof InvalidFormatException formato ? ": " + formato.getValue() : "";
            return responder(HttpStatus.BAD_REQUEST, "El campo " + campo + " tiene un valor inválido" + valor);
        }
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo del pedido no es un JSON válido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorVistaDTO> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorVistaDTO> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        Set<HttpMethod> aceptados = e.getSupportedHttpMethods();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(aceptados == null ? new HttpMethod[0] : aceptados.toArray(HttpMethod[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorVistaDTO("La ruta no acepta " + e.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorVistaDTO> formatoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo del pedido tiene que ser JSON");
    }

    // Un Accept que no admite JSON (application/xml): la respuesta de todos modos sale en JSON.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorVistaDTO> formatoNoAceptable(HttpMediaTypeNotAcceptableException e) {
        return responder(HttpStatus.NOT_ACCEPTABLE, "Esta API responde solo JSON");
    }

    // La URI tal como llegó: getResourcePath() viene sin la barra final, y "/api/salas/" decía
    // que no existe /api/salas, que sí existe.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorVistaDTO> rutaInexistente(NoResourceFoundException e, HttpServletRequest pedido) {
        return responder(HttpStatus.NOT_FOUND, "No existe la ruta " + pedido.getRequestURI());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorVistaDTO> falloDePersistencia(DataAccessException e) {
        log.error("Falló el acceso a los datos", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo acceder a los datos");
    }

    @ExceptionHandler(ComprobanteException.class)
    public ResponseEntity<ErrorVistaDTO> falloDeComprobante(ComprobanteException e) {
        log.error("Falló la emisión de un comprobante", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo emitir el comprobante");
    }

    // Sin esto Spring contesta con su propio formato y el front, que espera {error}, muestra vacío.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVistaDTO> errorInesperado(Exception e) {
        if (e instanceof ErrorResponse deSpring) {
            return responder(deSpring.getStatusCode(), deSpring.getBody().getDetail());
        }
        log.error("Error no previsto", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor");
    }

    // Content-Type fijo: así Spring no negocia contra el Accept. Con Accept: application/xml no
    // podía escribir el DTO y el error salía como un 500 vacío; con text/html, la página Whitelabel.
    private static ResponseEntity<ErrorVistaDTO> responder(HttpStatusCode estado, String mensaje) {
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorVistaDTO(mensaje));
    }
}
