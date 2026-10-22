package ar.uade.cine.infrastructure.imagenes.base;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.infrastructure.imagenes.AlmacenDeImagenes;
import ar.uade.cine.model.imagenes.FormatoImagen;
import ar.uade.cine.model.imagenes.NombreDeImagen;

class AlmacenDeImagenesEnBaseTest extends PruebaDeIntegracion {

    @Autowired
    private AlmacenDeImagenes almacen;

    @Test
    void loGuardadoSeLeeIgualYLoQueNoEstaNoDevuelveNada() {
        NombreDeImagen nombre = NombreDeImagen.nuevo(FormatoImagen.PNG);

        almacen.guardar(nombre, new byte[] {1, 2, 3});

        assertArrayEquals(new byte[] {1, 2, 3}, almacen.leer(nombre).orElseThrow());
        assertTrue(almacen.leer(NombreDeImagen.nuevo(FormatoImagen.PNG)).isEmpty());
    }
}
