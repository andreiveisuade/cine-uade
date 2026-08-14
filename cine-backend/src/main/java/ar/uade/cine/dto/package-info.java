/**
 * Los objetos con los que la API habla con el afuera: un tipo por forma de JSON, y nada más
 * que datos.
 *
 * <p>Son <strong>DTO</strong> —Data Transfer Object— y están separados del dominio a
 * propósito. Una {@code Pelicula} del dominio tiene comportamiento, invariantes y
 * asociaciones por id; una {@code PeliculaVistaDTO} no tiene nada de eso: es la película
 * aplanada tal como la necesita el front, con los enums ya pasados a texto y solo los
 * campos que se muestran. Serializar la entidad directamente ataría el contrato HTTP a la
 * forma interna del dominio, y entonces renombrar un atributo privado rompería el front.
 *
 * <p>Van en los dos sentidos, y el nombre lo dice:
 * <ul>
 *   <li>{@code Pedido*DTO} — lo que entra. Todos sus campos son objeto y no primitiva
 *       ({@code Integer}, no {@code int}) porque un campo ausente tiene que poder llegar
 *       en null: es lo que el gestor lee como «no lo mandé», y lo que dispara su mensaje
 *       de error en vez de uno que invente la capa HTTP.</li>
 *   <li>{@code *VistaDTO} — lo que sale. Ahí sí las primitivas, porque el dato ya existe.</li>
 * </ul>
 *
 * <p>Los subpaquetes espejan a {@code ar.uade.cine.dominio}: {@code cartelera},
 * {@code funciones}, {@code programaciones}, {@code salas}, {@code ventas}, {@code candy},
 * {@code promociones} y {@code usuarios}, más {@code catalogos} para los enums que el front
 * pide para armar sus formularios. Cada DTO queda al lado del paquete de dominio que
 * traduce, así que de qué entidad viene cada forma del JSON se lee en la ruta del archivo.
 * {@link ar.uade.cine.dto.ErrorVistaDTO} es el único que se queda en la raíz: no pertenece
 * a ninguna familia, es la forma de cualquier error.
 *
 * <p>No tienen lógica: quien los arma son las clases {@code Vistas*} de
 * {@link ar.uade.cine.api}, que son los ensambladores —van a buscar a los gestores lo que
 * el DTO necesita y no vive en una sola entidad—. Un DTO que se supiera armar solo tendría
 * que conocer los gestores, y volveríamos a mezclar transporte con negocio.
 */
package ar.uade.cine.dto;
