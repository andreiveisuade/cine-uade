package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.promociones.Promocion;

public interface PromocionDAO {

    void guardar(Promocion promocion);

    /** Para activarla o desactivarla: una promoción usada en un cobro no se borra. */
    void actualizar(Promocion promocion);

    Optional<Promocion> buscarPorId(int id);

    List<Promocion> listar();

    /** Las candidatas a aplicar en un cobro. Las inactivas ni se evalúan. */
    List<Promocion> listarActivas();

    void eliminar(int id);
}
