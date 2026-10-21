package ar.uade.cine.infrastructure.imagenes.disco;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.model.imagenes.FormatoImagen;
import ar.uade.cine.model.imagenes.NombreDeImagen;

class AlmacenDeImagenesEnDiscoTest {

    @TempDir
    Path carpeta;

    @Test
    void loGuardadoSeLeeIgualYLoQueNoEstaNoDevuelveNada() {
        AlmacenDeImagenesEnDisco almacen = new AlmacenDeImagenesEnDisco(carpeta.resolve("imagenes"));
        NombreDeImagen nombre = NombreDeImagen.nuevo(FormatoImagen.PNG);

        almacen.guardar(nombre, new byte[] {1, 2, 3});

        assertArrayEquals(new byte[] {1, 2, 3}, almacen.leer(nombre).orElseThrow());
        assertTrue(almacen.leer(NombreDeImagen.nuevo(FormatoImagen.PNG)).isEmpty());
    }

    // Segunda llave: aunque un nombre con «..» llegara hasta acá, no se lee afuera de la carpeta.
    @Test
    void unNombreQueSubeDeCarpetaNoSeLee() {
        AlmacenDeImagenesEnDisco almacen = new AlmacenDeImagenesEnDisco(carpeta.resolve("imagenes"));

        assertThrows(IllegalStateException.class,
                () -> almacen.leer(new NombreDeImagen("../secreto.png", FormatoImagen.PNG)));
    }
}
