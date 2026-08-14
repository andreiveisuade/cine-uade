package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.persistencia.ImportacionDAO;

/** Mismo contrato que la versión MySQL, en un mapa. Es la que usan los tests. */
public class ImportacionDAOMemoria implements ImportacionDAO {

    private final Map<Integer, Importacion> importaciones = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Importacion importacion) {
        importacion.setId(proximoId++);
        importaciones.put(importacion.getId(), importacion);
    }

    @Override
    public void actualizar(Importacion importacion) {
        importaciones.put(importacion.getId(), importacion);
    }

    @Override
    public List<Importacion> listarUltimas(int cuantas) {
        List<Importacion> ordenadas = new ArrayList<>(importaciones.values());
        // Por id descendente y no por fecha: dos corridas del mismo test caen en el mismo
        // milisegundo y el orden quedaria a suerte.
        ordenadas.sort(Comparator.comparingInt(Importacion::getId).reversed());
        return ordenadas.stream().limit(cuantas).toList();
    }
}
