package ar.uade.cine.infrastructure.imagenes.disco;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import ar.uade.cine.infrastructure.imagenes.AlmacenDeImagenes;
import ar.uade.cine.model.imagenes.NombreDeImagen;

// Guarda cada imagen como un archivo en una carpeta; en Docker es el volumen imagenes.
public class AlmacenDeImagenesEnDisco implements AlmacenDeImagenes {

    private final Path directorio;

    public AlmacenDeImagenesEnDisco(Path directorio) {
        this.directorio = directorio.toAbsolutePath().normalize();
    }

    @Override
    public void guardar(NombreDeImagen nombre, byte[] contenido) {
        try {
            Files.createDirectories(directorio);
            Files.write(archivo(nombre), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen " + nombre.valor(), e);
        }
    }

    @Override
    public Optional<byte[]> leer(NombreDeImagen nombre) {
        try {
            return Optional.of(Files.readAllBytes(archivo(nombre)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen " + nombre.valor(), e);
        }
    }

    // NombreDeImagen ya no admite «..» ni «/»; esto es la segunda llave por si eso cambia.
    private Path archivo(NombreDeImagen nombre) {
        Path archivo = directorio.resolve(nombre.valor()).normalize();
        if (!archivo.getParent().equals(directorio)) {
            throw new IllegalStateException("La imagen " + nombre.valor() + " cae fuera de su carpeta");
        }
        return archivo;
    }
}
