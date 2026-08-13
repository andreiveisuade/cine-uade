package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.persistencia.PromocionDAO;

/** Mismo contrato que la versión MySQL, en un mapa. Es la que usan los tests. */
public class PromocionDAOMemoria implements PromocionDAO {

    private final Map<Integer, Promocion> promociones = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Promocion promocion) {
        promocion.setId(proximoId++);
        promociones.put(promocion.getId(), promocion);
    }

    @Override
    public void actualizar(Promocion promocion) {
        promociones.put(promocion.getId(), promocion);
    }

    @Override
    public Optional<Promocion> buscarPorId(int id) {
        return Optional.ofNullable(promociones.get(id));
    }

    @Override
    public List<Promocion> listar() {
        return new ArrayList<>(promociones.values());
    }

    @Override
    public List<Promocion> listarActivas() {
        return promociones.values().stream().filter(Promocion::estaActiva).toList();
    }

    @Override
    public void eliminar(int id) {
        promociones.remove(id);
    }
}
