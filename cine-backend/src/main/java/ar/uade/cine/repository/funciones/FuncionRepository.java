package ar.uade.cine.repository.funciones;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.repository.Repositorio;

// Persistencia de funciones; Repository de Spring Data, con los filtros opcionales armados con la Criteria
// API de JPA (Specification) en vez de un JPQL a mano.
public interface FuncionRepository extends Repositorio<Funcion>, JpaSpecificationExecutor<Funcion> {

    List<Funcion> findByPelicula_IdOrderByInicioAsc(int peliculaId);

    List<Funcion> findBySala_IdAndInicioBetween(int salaId, LocalDateTime desde, LocalDateTime hasta);

    List<Funcion> findByProgramacionId(int programacionId);

    boolean existsByPelicula_Id(int peliculaId);

    boolean existsBySala_Id(int salaId);

    // Cada filtro en null no entra al WHERE; `hasta` es exclusivo.
    default List<Funcion> buscar(Integer peliculaId, Integer salaId, LocalDateTime desde, LocalDateTime hasta) {
        Specification<Funcion> filtros = Specification.allOf(
                deLaPelicula(peliculaId), deLaSala(salaId), desde(desde), antesDe(hasta));
        return findAll(filtros, Sort.by("inicio", "id"));
    }

    private static Specification<Funcion> deLaPelicula(Integer peliculaId) {
        return (f, consulta, cb) -> peliculaId == null ? null : cb.equal(f.get("pelicula").get("id"), peliculaId);
    }

    private static Specification<Funcion> deLaSala(Integer salaId) {
        return (f, consulta, cb) -> salaId == null ? null : cb.equal(f.get("sala").get("id"), salaId);
    }

    private static Specification<Funcion> desde(LocalDateTime desde) {
        return (f, consulta, cb) -> desde == null ? null : cb.greaterThanOrEqualTo(f.get("inicio"), desde);
    }

    private static Specification<Funcion> antesDe(LocalDateTime hasta) {
        return (f, consulta, cb) -> hasta == null ? null : cb.lessThan(f.get("inicio"), hasta);
    }
}
