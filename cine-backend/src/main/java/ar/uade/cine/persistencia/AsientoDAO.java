package ar.uade.cine.persistencia;

import java.util.List;

import ar.uade.cine.dominio.salas.Asiento;

public interface AsientoDAO {

    /** Alta en lote: los asientos de una sala se generan todos juntos. */
    void guardarTodos(List<Asiento> asientos);

    /** Para marcar una butaca fuera de servicio o reponerla. */
    void actualizar(Asiento asiento);

    List<Asiento> listarPorSala(int salaId);
}
