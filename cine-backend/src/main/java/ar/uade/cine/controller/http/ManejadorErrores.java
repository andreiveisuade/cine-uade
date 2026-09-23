package ar.uade.cine.controller.http;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
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

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

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
    // controller/ no puede nombrar jakarta.persistence (ArquitecturaTest). Va antes que el
    // 500 de DataAccessException porque es su subclase.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorVistaDTO> conflictoDeVersion(OptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "La reserva cambió mientras se procesaba: volvé a intentarlo");
    }

    // En la ruta es un recurso que no existe (404); en la query, un dato mal escrito (400).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorVistaDTO> identificadorInvalido(MethodArgumentTypeMismatchException e) {
        if (e.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return responder(HttpStatus.NOT_FOUND, "El identificador " + e.getValue() + " no es válido");
        }
        return responder(HttpStatus.BAD_REQUEST, "El parámetro " + e.getName() + " no es válido: " + e.getValue());
    }

    // Un JSON bien formado con un tipo equivocado ("precio": "abc") no es "JSON inválido": el
    // mensaje nombra el campo, que es lo que el usuario puede corregir.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorVistaDTO> cuerpoIlegible(HttpMessageNotReadableException e) {
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

    // Sin esto Spring contesta con su propio formato y el front, que espera {error}, muestra vacío.
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
