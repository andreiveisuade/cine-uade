package ar.uade.cine.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ar.uade.cine.dto.candy.PedidoComboDTO;
import ar.uade.cine.dto.candy.PedidoEdicionProductoDTO;
import ar.uade.cine.dto.candy.PedidoProductoDTO;
import ar.uade.cine.dto.funciones.PedidoFuncionDTO;
import ar.uade.cine.dto.programaciones.PedidoGrillaDTO;
import ar.uade.cine.dto.programaciones.PedidoProgramacionDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

// Los seis pedidos con precio dicen lo mismo que Dinero.importeValido, que es lo que ve el alta sin HTTP:
// el texto de un problema no depende de por dónde entró el pedido.
class PrecioDeLosPedidosTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    static Stream<Arguments> pedidos() {
        return Stream.of(
                pedido("función", precio -> new PedidoFuncionDTO(1, 1, "2026-09-01T20:00:00", "SUBTITULADA",
                        "DOS_D", precio)),
                pedido("programación", precio -> new PedidoProgramacionDTO(1, 1, "2026-09-01", null, "20:00",
                        List.of(), "SUBTITULADA", "DOS_D", precio)),
                pedido("grilla", precio -> new PedidoGrillaDTO(null, null, null, null, null, precio, null, null)));
    }

    private static Arguments pedido(String nombre, Function<Double, Object> conPrecio) {
        return Arguments.of(nombre, conPrecio);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pedidos")
    void sinPrecioDiceQueFalta(String nombre, Function<Double, Object> conPrecio) {
        assertEquals(Set.of("Falta el precio"), errores(conPrecio.apply(null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pedidos")
    void conPrecioEnCeroDiceQueTieneQueSerMayor(String nombre, Function<Double, Object> conPrecio) {
        assertEquals(Set.of("El precio tiene que ser mayor a cero"), errores(conPrecio.apply(0.0)));
    }

    private static Set<String> errores(Object pedido) {
        return VALIDADOR.validate(pedido).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    // Los del candy solo exigen que el precio venga: el valor lo valida Dinero.importe al convertirlo en el
    // controller, con los mismos textos (ProductoControllerTest los prueba por HTTP).
    static Stream<Arguments> pedidosDelCandy() {
        return Stream.of(
                pedido("producto", precio -> new PedidoProductoDTO("Agua", "BEBIDA", precio)),
                pedido("combo", precio -> new PedidoComboDTO("Combo", precio, Map.of())),
                pedido("edición de producto", precio -> new PedidoEdicionProductoDTO("Agua", precio)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pedidosDelCandy")
    void enElCandyElPedidoSoloExigeQueElPrecioVenga(String nombre, Function<Double, Object> conPrecio) {
        assertEquals(Set.of("Falta el precio"), errores(conPrecio.apply(null)));
        assertEquals(Set.of(), errores(conPrecio.apply(0.0)));
    }
}
