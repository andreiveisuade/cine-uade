package ar.uade.cine.dto.ventas;

import java.util.List;

/**
 * Qué butacas quedaron a nombre de esa sesión y cuáles se le escaparon.
 *
 * <p>{@code rechazadas} viaja aparte en vez de responder un error porque perder una butaca
 * mientras se elige no es una falla del pedido: las otras sí se consiguieron, y quien
 * pregunta necesita las dos listas para repintar el mapa sin volver a pedirlo.
 *
 * <p>{@code vencenEnSegundos} evita que el navegador tenga que saber cuánto dura un
 * bloqueo: la ventana es una regla del negocio y puede cambiar sin tocar el frontend.
 */
public record BloqueoVistaDTO(String sesion, List<String> butacas, List<String> rechazadas,
                              long vencenEnSegundos) {
}
