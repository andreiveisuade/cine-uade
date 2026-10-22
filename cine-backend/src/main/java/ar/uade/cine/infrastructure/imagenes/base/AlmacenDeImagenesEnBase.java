package ar.uade.cine.infrastructure.imagenes.base;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import ar.uade.cine.infrastructure.imagenes.AlmacenDeImagenes;
import ar.uade.cine.model.imagenes.ArchivoDeImagen;
import ar.uade.cine.model.imagenes.NombreDeImagen;
import ar.uade.cine.repository.imagenes.ArchivoDeImagenRepository;

// Guarda cada imagen en la tabla imagen; reemplazó al disco porque el servicio gratis donde se despliega no
// tiene disco que sobreviva a un reinicio, y así un dump de la base se lleva también las imágenes.
@RequiredArgsConstructor
public class AlmacenDeImagenesEnBase implements AlmacenDeImagenes {

    private final ArchivoDeImagenRepository imagenes;

    @Override
    public void guardar(NombreDeImagen nombre, byte[] contenido) {
        imagenes.save(new ArchivoDeImagen(nombre, contenido));
    }

    @Override
    public Optional<byte[]> leer(NombreDeImagen nombre) {
        return imagenes.findById(nombre.valor()).map(ArchivoDeImagen::getContenido);
    }
}
