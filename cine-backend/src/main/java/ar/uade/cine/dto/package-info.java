/**
 * La forma de cada JSON de la API, sin lógica. Separados del dominio para que renombrar un
 * atributo de una entidad no rompa el front.
 *
 * <p>{@code Pedido*DTO} es lo que entra: campos objeto y no primitiva, para que un ausente
 * llegue en null y el error lo dé el gestor. {@code *VistaDTO} es lo que sale. Los arman las
 * clases {@code Vistas*} de {@link ar.uade.cine.controller}.
 */
package ar.uade.cine.dto;
