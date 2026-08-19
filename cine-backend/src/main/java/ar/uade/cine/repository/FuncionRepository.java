package ar.uade.cine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.funciones.Funcion;

/**
 * Las funciones programadas. Las tres búsquedas son las tres preguntas que se le hacen: qué
 * pasa esta película, qué pasa en esta sala, y qué generó esta grilla.
 */
public interface FuncionRepository extends JpaRepository<Funcion, Integer> {

    List<Funcion> findByPeliculaId(int peliculaId);

    List<Funcion> findBySalaId(int salaId);

    List<Funcion> findByProgramacionId(int programacionId);
}
