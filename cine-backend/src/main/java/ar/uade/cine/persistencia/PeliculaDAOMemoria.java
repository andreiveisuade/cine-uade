package ar.uade.cine.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;

/**
 * Implementación en memoria: sirve para probar la lógica sin levantar MySQL.
 */
public class PeliculaDAOMemoria implements PeliculaDAO {

    private final Map<Integer, Pelicula> peliculas = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Pelicula pelicula) {
        pelicula.setId(proximoId++);
        peliculas.put(pelicula.getId(), pelicula);
    }

    @Override
    public Optional<Pelicula> buscarPorId(int id) {
        return Optional.ofNullable(peliculas.get(id));
    }

    @Override
    public List<Pelicula> listar() {
        return new ArrayList<>(peliculas.values());
    }

    @Override
    public void eliminar(int id) {
        peliculas.remove(id);
    }
}
