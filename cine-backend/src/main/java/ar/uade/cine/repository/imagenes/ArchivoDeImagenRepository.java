package ar.uade.cine.repository.imagenes;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.imagenes.ArchivoDeImagen;

// Persistencia de las imágenes subidas; por nombre y no por id numérico, que es como las pide la URL.
public interface ArchivoDeImagenRepository extends JpaRepository<ArchivoDeImagen, String> {
}
