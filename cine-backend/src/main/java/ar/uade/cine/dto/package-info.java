/**
 * Pedido*DTO usa tipos objeto: un ausente llega en null y no como un 0 que pase por dato.
 * Bean Validation rechaza lo que falta o viene mal formado con el mismo texto que daría el gestor,
 * y ManejadorErrores lo devuelve como 400; la entidad o el gestor lo vuelven a validar para quien
 * no entra por HTTP, como el importador y los tests.
 */
package ar.uade.cine.dto;
