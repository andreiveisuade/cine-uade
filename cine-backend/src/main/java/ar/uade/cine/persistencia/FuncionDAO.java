package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;

public interface FuncionDAO {

    void guardar(Funcion funcion);

    Optional<Funcion> buscarPorId(int id);

    List<Funcion> listar();

    /** Para armar la cartelera de una película. */
    List<Funcion> listarPorPelicula(int peliculaId);

    /** Para detectar funciones superpuestas antes de programar una nueva. */
    List<Funcion> listarPorSala(int salaId);

    void eliminar(int id);
}
