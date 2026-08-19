package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.cartelera.Importacion;

/**
 * El historial de corridas del importador.
 *
 * <p>Se lee siempre acotado: la lista solo crece y la pantalla muestra las últimas veinte.
 * Traerlas todas para descartar las viejas sería pedirle a la base el trabajo de un año para
 * dibujar media pantalla.
 */
public interface ImportacionRepository extends JpaRepository<Importacion, Integer> {

    List<Importacion> findAllByOrderByIdDesc(Limit cuantas);
}
