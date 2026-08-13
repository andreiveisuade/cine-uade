package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Sala;

/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 * Guarda la sala en sí (nombre, tipo, distribución de filas); sus butacas concretas
 * son responsabilidad de AsientoDAO, que las maneja aparte por sala.
 */
public interface SalaDAO {

    void guardar(Sala sala);

    Optional<Sala> buscarPorId(int id);

    List<Sala> listar();

    void eliminar(int id);
}
