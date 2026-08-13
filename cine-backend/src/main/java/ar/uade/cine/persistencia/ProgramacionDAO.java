package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.programaciones.Programacion;

/**
 * El contrato de la grilla. No hay {@code eliminar}: una programación que ya generó
 * funciones no se borra, se da de baja — es el mismo criterio que promoción y producto.
 */
public interface ProgramacionDAO {

    void guardar(Programacion programacion);

    /** Para darla de alta o de baja: es lo único que el ABM deja cambiar. */
    void actualizar(Programacion programacion);

    Optional<Programacion> buscarPorId(int id);

    List<Programacion> listar();

    List<Programacion> listarActivas();
}
