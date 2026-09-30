package ar.uade.cine.model.ventas.validacion;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.validacion.Regla;
import ar.uade.cine.model.ventas.Entrada;

// Qué trae una reserva al nacer: función, cliente, fecha y de 1 a 10 butacas sin repetir; guardas de Regla.
// Lo llama Reserva y nadie más: la regla es de la entidad aunque se lea en su propia clase.
public final class ValidadorReserva {

    // Lo que compra una familia o un grupo. Sin tope, un pedido retenía la sala entera durante el pago.
    public static final int MAXIMO_BUTACAS = 10;

    private static final String TOPE = "Una compra tiene que tener como máximo " + MAXIMO_BUTACAS + " butacas";

    private ValidadorReserva() {
    }

    public static void validar(Funcion funcion, Cliente cliente, List<Entrada> entradas, LocalDateTime creadaEn) {
        Regla.objeto(funcion).obligatorio("Falta la función");
        Regla.objeto(cliente).obligatorio("Falta el cliente");
        Regla.objeto(creadaEn).obligatorio("Falta la fecha y hora de la reserva");
        Regla.lista(entradas).noVacia("Hay que elegir al menos una butaca").hasta(MAXIMO_BUTACAS, TOPE);
        repetida(entradas).ifPresent(codigo -> {
            throw new DatoInvalido("La butaca " + codigo + " está repetida en el pedido");
        });
    }

    // El mismo tope para el bloqueo, que aparta las butacas antes de que la reserva exista.
    public static void validarTope(Collection<?> butacas) {
        Regla.lista(butacas).hasta(MAXIMO_BUTACAS, TOPE);
    }

    // "a1" y "A1" son dos claves del pedido y la misma butaca. Se comparan por el código de la butaca, que
    // ya viene normalizado: sin esto las dos entradas chocaban contra el UNIQUE y el cliente leía un 409
    // de butaca tomada por otro.
    private static Optional<String> repetida(List<Entrada> entradas) {
        Map<String, Long> porCodigo = entradas.stream()
                .collect(Collectors.groupingBy(Entrada::codigoAsiento, LinkedHashMap::new, Collectors.counting()));
        return porCodigo.entrySet().stream()
                .filter(codigo -> codigo.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
