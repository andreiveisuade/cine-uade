package ar.uade.cine.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;

public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {

    boolean existsByTituloIgnoreCaseAndIdNot(String titulo, int id);

    List<Pelicula> findByEstadoRevision(EstadoRevision estado);

    @Query("""
            select p from Pelicula p
            where p.enCartelera = true
              and (:genero is null or :genero member of p.generos)
              and exists (select f from Funcion f where f.pelicula = p and f.inicio > :ahora)
            order by p.id""")
    List<Pelicula> findEnCartelera(@Param("ahora") LocalDateTime ahora, @Param("genero") Genero genero);

    // locate y no like: el texto del usuario puede traer % o _.
    @Query("""
            select p from Pelicula p
            where (:texto = '' or locate(:texto, lower(p.titulo)) > 0)
              and (:genero is null or :genero member of p.generos)
              and (:publicada is null or p.enCartelera = :publicada)
            order by p.id""")
    List<Pelicula> buscar(@Param("texto") String texto, @Param("genero") Genero genero,
                          @Param("publicada") Boolean publicada);
}
