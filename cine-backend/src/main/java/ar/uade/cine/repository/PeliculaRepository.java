package ar.uade.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.Pelicula;

/**
 * El catálogo de películas.
 *
 * <p>No declara ningún método propio y no está vacío por eso: {@link JpaRepository} ya trae
 * guardar, buscar por id, listar y borrar, que es todo lo que el catálogo necesita. Los
 * filtros de la búsqueda del encargado —por título, género y publicada— los resuelve
 * {@code GestorCartelera} sobre la lista, como siempre: son tres condiciones opcionales que
 * se combinan, y expresarlas acá serían ocho consultas o una consulta con tres OR que no se
 * puede leer.
 *
 * <p>Esta interfaz es lo que reemplazó al par {@code PeliculaDAO} + {@code PeliculaDAOMySQL}
 * de 130 líneas de SQL. La implementación la genera Spring Data en tiempo de arranque, y la
 * inversión de dependencias sigue siendo la misma de antes: el gestor depende de esta
 * interfaz y no sabe qué hay del otro lado.
 */
public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {
}
