package ar.uade.cine.controller.http;

import java.lang.reflect.RecordComponent;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ar.uade.cine.controller.http.cuerpo.CampoConValorInvalido;
import ar.uade.cine.controller.http.cuerpo.JsonMalFormado;
import ar.uade.cine.controller.http.cuerpo.LecturaDelCuerpo;
import ar.uade.cine.controller.http.cuerpo.RaizQueNoEsObjeto;
import ar.uade.cine.controller.http.cuerpo.SinCuerpo;
import ar.uade.cine.dto.comun.ErrorVistaDTO;
import ar.uade.cine.model.imagenes.Imagen;
import ar.uade.cine.model.rechazos.ButacaOcupada;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.DemasiadosIntentos;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

import jakarta.servlet.http.HttpServletRequest;

// Traduce cada excepción a status HTTP y JSON {error}; @RestControllerAdvice: ningún controller atrapa.
// Spring elige el handler de la excepción más cercana en la jerarquía: el orden de los métodos no importa.
@RestControllerAdvice
@Slf4j
public class ManejadorErrores {

    private static final String ERROR_INESPERADO = "Ocurrió un error inesperado en el servidor";

    // Chain of Responsibility: el orden de la lista es la regla; el último contesta lo que ninguno reconoció.
    private static final List<LecturaDelCuerpo> CADENA = List.of(
            new SinCuerpo(), new CampoConValorInvalido(), new RaizQueNoEsObjeto(), new JsonMalFormado());

    // Los rechazos: el texto lo escribió quien rechazó, para el usuario, y sale intacto.
    // Los 409 van al log (ningún texto nombra a una persona); los 400 no: son ruido.

    @ExceptionHandler(DatoInvalido.class)
    public ResponseEntity<ErrorVistaDTO> datoInvalido(DatoInvalido e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontrado.class)
    public ResponseEntity<ErrorVistaDTO> noEncontrado(RecursoNoEncontrado e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoDeNegocio.class)
    public ResponseEntity<ErrorVistaDTO> conflicto(ConflictoDeNegocio e) {
        log.info("rechazo 409 · {}", e.getMessage());
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DemasiadosIntentos.class)
    public ResponseEntity<ErrorVistaDTO> demasiadosIntentos(DemasiadosIntentos e) {
        return responder(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    // Otro ganó la butaca (R4): la web vuelve al mapa recargado solo ante un 409.
    @ExceptionHandler(ButacaOcupada.class)
    public ResponseEntity<ErrorVistaDTO> butacaOcupada(ButacaOcupada e) {
        log.info("rechazo 409 · {}", e.getMessage());
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    // La que no es Rechazo la tiró una librería o un bug: su texto es técnico y no es para el usuario.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorVistaDTO> argumentoNoPrevisto(IllegalArgumentException e) {
        log.error("Una IllegalArgumentException que no es un Rechazo", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INESPERADO);
    }

    // Lo que Spring rechaza antes de llegar al controller.

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

    // Dos operaciones a la vez sobre la misma reserva: la segunda pierde. Gana sobre el 500 de DataAccessException.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorVistaDTO> conflictoDeVersion(OptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "La reserva cambió mientras se procesaba: volvé a intentarlo");
    }

    // Carrera contra un UNIQUE tras pasar la validación del gestor; la causa nombra la restricción y va al log.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorVistaDTO> conflictoDeIntegridad(DataIntegrityViolationException e) {
        log.warn("Un pedido chocó con una restricción de la base: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT,
                "Otro pedido cambió estos datos al mismo tiempo: recargá y volvé a intentarlo");
    }

    // Fecha que pasó Parseo y desborda en la aritmética de un gestor: es del pedido, no un 500; detalle al log.
    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ErrorVistaDTO> fechaFueraDeRango(DateTimeException e) {
        log.warn("Una fecha del pedido desbordó: {}", e.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "Una de las fechas del pedido no es válida");
    }

    // Solo las variables de ruta llegan tipadas; la query viaja como String y la lee Parseo.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorVistaDTO> identificadorInvalido(MethodArgumentTypeMismatchException e,
                                                               HttpServletRequest pedido) {
        return responder(HttpStatus.NOT_FOUND, "No existe la ruta " + pedido.getRequestURI());
    }

    // Qué corregir en un cuerpo que no se pudo leer lo decide la cadena (ver LecturaDelCuerpo).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorVistaDTO> cuerpoIlegible(HttpMessageNotReadableException e) {
        String mensaje = CADENA.stream().map(l -> l.mensaje(e)).flatMap(Optional::stream).findFirst().orElseThrow();
        return responder(HttpStatus.BAD_REQUEST, mensaje);
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

    // getResourcePath() viene sin la barra final: /api/salas/ decía que no existe /api/salas.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorVistaDTO> rutaInexistente(NoResourceFoundException e, HttpServletRequest pedido) {
        return responder(HttpStatus.NOT_FOUND, "No existe la ruta " + pedido.getRequestURI());
    }

    // Pasado el tope de multipart el archivo ni se lee: el mismo 400 que da Imagen hasta ese tope.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorVistaDTO> archivoDemasiadoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.BAD_REQUEST, Imagen.DEMASIADO_PESADA);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorVistaDTO> falloDePersistencia(DataAccessException e) {
        log.error("Falló el acceso a los datos", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo acceder a los datos");
    }

    // Sin esto Spring contesta en su formato y el front, que espera {error}, muestra vacío.
    // Conserva el status pero su detail viene en inglés o vacío: se reemplaza y el detail va al log.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVistaDTO> errorInesperado(Exception e) {
        if (e instanceof ErrorResponse deSpring) {
            log.warn("Spring rechazó el pedido con {}: {}", deSpring.getStatusCode().value(),
                    deSpring.getBody().getDetail());
            return responder(deSpring.getStatusCode(), textoDe(deSpring.getStatusCode()));
        }
        log.error("Error no previsto", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INESPERADO);
    }

    // Por status y no por excepción: Spring tiene muchas; ErroresController usa los mismos.
    static String textoDe(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 404 -> "No existe lo que se pidió";
            case 503 -> "El servidor no está disponible: volvé a intentarlo en un rato";
            default -> estado.is4xxClientError() ? "El pedido no es válido" : ERROR_INESPERADO;
        };
    }

    // Content-Type fijo: con Accept: application/xml el error salía como 500 vacío.
    static ResponseEntity<ErrorVistaDTO> responder(HttpStatusCode estado, String mensaje) {
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorVistaDTO(mensaje));
    }
}
