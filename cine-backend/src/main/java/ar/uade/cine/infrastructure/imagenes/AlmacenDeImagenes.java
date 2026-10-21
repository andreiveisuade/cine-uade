package ar.uade.cine.infrastructure.imagenes;

import java.util.Optional;

import ar.uade.cine.model.imagenes.NombreDeImagen;

// Puerto: dónde quedan las imágenes subidas; GestorImagenes no sabe si es un disco o un bucket.
public interface AlmacenDeImagenes {

    void guardar(NombreDeImagen nombre, byte[] contenido);

    Optional<byte[]> leer(NombreDeImagen nombre);
}
